package com.github.jpmand.idea.plugin.gitea.pullrequest.editor

import com.github.jpmand.idea.plugin.gitea.pullrequest.GiteaPullRequestsSettings
import com.github.jpmand.idea.plugin.gitea.pullrequest.data.GiteaPRForCurrentBranch
import com.github.jpmand.idea.plugin.gitea.pullrequest.data.GiteaPRForCurrentBranchService
import com.github.jpmand.idea.plugin.gitea.pullrequest.diff.GiteaPRChangedFile
import com.github.jpmand.idea.plugin.gitea.pullrequest.ui.editor.GiteaPRDiffEditorModel
import com.github.jpmand.idea.plugin.gitea.pullrequest.ui.editor.GiteaPRInlayComponentsFactory
import com.github.jpmand.idea.plugin.gitea.pullrequest.ui.editor.GiteaPRLiveDiffSync
import com.github.jpmand.idea.plugin.gitea.pullrequest.ui.launchReviewToolbar
import com.github.jpmand.idea.plugin.gitea.util.GiteaUtil
import com.intellij.collaboration.ui.codereview.diff.DiffLineLocation
import com.intellij.collaboration.ui.codereview.diff.viewer.showCodeReview
import com.intellij.diff.util.Side
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorKind
import com.intellij.openapi.editor.event.EditorFactoryEvent
import com.intellij.openapi.editor.event.EditorFactoryListener
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.vcsUtil.VcsFileUtil
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * Mirrors the bundled GitHub plugin's `GHPRReviewInEditorController`: shows the same review
 * gutter icons/inlays/compose-and-reply UI the diff tab has ([GiteaPRDiffEditorModel],
 * [GiteaPRInlayComponentsFactory]) in a *regular* project editor, when that editor's file belongs
 * to the PR whose head branch matches the current local branch
 * ([GiteaPRForCurrentBranchService]). Entirely separate wiring from
 * [com.github.jpmand.idea.plugin.gitea.pullrequest.diff.GiteaPRDiffExtension]'s `DiffExtension`
 * path — this fires for every editor the platform creates, not just diff viewers.
 *
 * Only comments anchored to the PR's *new* side ([GiteaPRThreadViewModel.newLine][com.github.jpmand.idea.plugin.gitea.pullrequest.review.GiteaPRThreadViewModel.newLine])
 * are shown — a comment on a since-removed base-side line has no line to anchor to in the current
 * working-tree file at all, so those stay diff-tab-only, matching the bundled GitHub plugin's own
 * in-editor scope.
 *
 * Registered via `<editorFactoryListener>` in plugin.xml.
 */
class GiteaPRReviewInEditorController : EditorFactoryListener {

    private val editorJobs = ConcurrentHashMap<Editor, Job>()

    override fun editorCreated(event: EditorFactoryEvent) {
        val editor = event.editor
        if (editor.editorKind != EditorKind.MAIN_EDITOR) return
        if (editor !is EditorEx) return
        val project = editor.project ?: return
        if (!project.service<GiteaPullRequestsSettings>().editorReviewEnabled) return
        val virtualFile = FileDocumentManager.getInstance().getFile(editor.document) ?: return

        val job = SupervisorJob()
        editorJobs[editor] = job
        val cs = CoroutineScope(job + Dispatchers.Main)

        cs.launch {
            project.service<GiteaPRForCurrentBranchService>().current.collectLatest { current ->
                if (current == null) return@collectLatest
                val changedFile = matchFile(current, virtualFile) ?: return@collectLatest
                wireReview(cs, project, editor, current, changedFile)
            }
        }
    }

    override fun editorReleased(event: EditorFactoryEvent) {
        editorJobs.remove(event.editor)?.cancel()
    }

    private fun matchFile(current: GiteaPRForCurrentBranch, virtualFile: VirtualFile): GiteaPRChangedFile? {
        if (!VfsUtilCore.isAncestor(current.gitRepositoryRoot, virtualFile, false)) return null
        val relativePath = VcsFileUtil.getRelativeFilePath(virtualFile, current.gitRepositoryRoot)
        return current.changedFiles.firstOrNull { it.filename == relativePath }
    }

    /**
     * Suspends until the enclosing `collectLatest` moves to a new value (branch/PR changed, or
     * this editor no longer matches) — same per-editor lifecycle shape as
     * [com.github.jpmand.idea.plugin.gitea.pullrequest.diff.GiteaPRDiffExtension.onViewerCreated]'s
     * `showCodeReview` call for a diff viewer.
     */
    private suspend fun wireReview(
        cs: CoroutineScope,
        project: Project,
        editor: EditorEx,
        current: GiteaPRForCurrentBranch,
        changedFile: GiteaPRChangedFile,
    ) {
        val headContent = try {
            current.repository.loadFileContent(changedFile.filename, current.pr.head.sha)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            GiteaUtil.LOG.warn("Failed to load head content for '${changedFile.filename}'", e)
            return
        }

        val sync = GiteaPRLiveDiffSync(cs, headContent, editor.document)
        // Always Side.RIGHT: the live document descends from the head-SHA snapshot plus local
        // edits, so it only ever has a "new side" — see the class doc on base-side comments.
        val locationToLine: (DiffLineLocation) -> Int? = { (side, anchorLine) ->
            if (side == Side.RIGHT) sync.anchorToLive(anchorLine) else null
        }
        val lineToLocation: (Int) -> DiffLineLocation? = { liveLine ->
            sync.liveToAnchor(liveLine)?.let { Pair(Side.RIGHT, it) }
        }

        cs.launchReviewToolbar(project, editor, current.discussionsVm)
        val model = GiteaPRDiffEditorModel(
            cs, project, changedFile, Side.RIGHT, current.discussionsVm, locationToLine, lineToLocation, editor,
        )
        editor.showCodeReview(model) { inlayModel ->
            GiteaPRInlayComponentsFactory.createRenderer(project, cs, inlayModel, current.discussionsVm)
        }
    }
}
