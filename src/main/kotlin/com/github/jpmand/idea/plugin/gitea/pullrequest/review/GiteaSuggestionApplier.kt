package com.github.jpmand.idea.plugin.gitea.pullrequest.review

import com.github.jpmand.idea.plugin.gitea.pullrequest.data.GiteaPRForCurrentBranchService
import com.github.jpmand.idea.plugin.gitea.pullrequest.ui.editor.GiteaPRLiveDiffSync
import com.github.jpmand.idea.plugin.gitea.util.GiteaBundle
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.readAction
import com.intellij.openapi.command.writeCommandAction
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageDialogBuilder
import com.intellij.openapi.util.TextRange
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob

/**
 * Applies a suggested change to the local working copy: locates the file via the current
 * branch's PR mapping ([GiteaPRForCurrentBranchService]), maps the suggestion's head-SHA-anchored
 * line range onto the live document (drift-corrected via a one-off [GiteaPRLiveDiffSync] — the
 * same mapping Phase 9's live editor keeps running continuously, recomputed fresh here since this
 * can be triggered from a surface, like the Timeline, with no live session running at all),
 * verifies the current content still looks like [GiteaSuggestion.oldLines], and replaces it with
 * [GiteaSuggestion.newLines].
 */
@Suppress("UnstableApiUsage")
suspend fun applySuggestion(cs: CoroutineScope, project: Project, path: String, suggestion: GiteaSuggestion) {
    val current = project.service<GiteaPRForCurrentBranchService>().current.value
    val changedFile = current?.changedFiles?.firstOrNull { it.filename == path }
    if (current == null || changedFile == null) {
        notifyApplyFailure(project, "pull.request.action.apply.suggestion.error.no.pr")
        return
    }

    val virtualFile = current.gitRepositoryRoot.findFileByRelativePath(path)
    val document = virtualFile?.let { vf -> readAction { FileDocumentManager.getInstance().getDocument(vf) } }
    if (document == null) {
        notifyApplyFailure(project, "pull.request.action.apply.suggestion.error.no.file")
        return
    }

    val headContent = try {
        current.repository.loadFileContent(path, current.pr.head.sha)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        notifyApplyFailure(project, "pull.request.action.apply.suggestion.error.no.file")
        return
    }

    val syncJob = SupervisorJob(cs.coroutineContext[Job])
    try {
        val sync = GiteaPRLiveDiffSync(CoroutineScope(cs.coroutineContext + syncJob), headContent, document)
        val liveStart = sync.anchorToLive(suggestion.oldStartLine)
        val liveEnd = sync.anchorToLive(suggestion.oldStartLine + suggestion.oldLines.size)

        val currentLines = readAction {
            (liveStart until liveEnd).map { line ->
                document.getText(TextRange(document.getLineStartOffset(line), document.getLineEndOffset(line)))
            }
        }
        if (currentLines != suggestion.oldLines) {
            val proceed = MessageDialogBuilder.yesNo(
                GiteaBundle.message("pull.request.action.apply.suggestion.confirm.title"),
                GiteaBundle.message("pull.request.action.apply.suggestion.confirm.message"),
            ).asWarning().ask(project)
            if (!proceed) return
        }

        val replacement = if (suggestion.newLines.isEmpty()) "" else suggestion.newLines.joinToString("\n") + "\n"
        writeCommandAction(project, GiteaBundle.message("pull.request.action.apply.suggestion")) {
            val startOffset = document.getLineStartOffset(liveStart)
            val endOffset = if (liveEnd < document.lineCount) document.getLineStartOffset(liveEnd) else document.textLength
            document.replaceString(startOffset, endOffset, replacement)
        }
        FileEditorManager.getInstance(project).openTextEditor(OpenFileDescriptor(project, virtualFile, liveStart, 0), true)
    } finally {
        syncJob.cancel()
    }
}

private fun notifyApplyFailure(project: Project, bundleKey: String) {
    NotificationGroupManager.getInstance()
        .getNotificationGroup("Gitea")
        .createNotification(GiteaBundle.message("pull.request.action.apply.suggestion"), GiteaBundle.message(bundleKey), NotificationType.ERROR)
        .notify(project)
}
