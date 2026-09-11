package com.github.jpmand.idea.plugin.gitea.pullrequest

import com.github.jpmand.idea.plugin.gitea.api.models.GiteaPullRequest
import com.github.jpmand.idea.plugin.gitea.pullrequest.data.GiteaPRDataContext
import com.github.jpmand.idea.plugin.gitea.pullrequest.data.GiteaPRRepository
import com.intellij.openapi.project.Project
import com.intellij.testFramework.LightVirtualFile

/**
 * The PR "activity timeline" (Conversation) as an editor tab — see [GiteaPRTimelineFileEditor] /
 * [GiteaPRTimelineEditorProvider]. Opened from the "Show Conversation" link in the details
 * tool-window tab. A bare [LightVirtualFile] (like [com.github.jpmand.idea.plugin.gitea.pullrequest.diff.GiteaPRDiffVirtualFile]).
 *
 * [repository] / [pr] / [ctx] are captured by value; if the account/repo context changes while
 * this tab is open, the OPEN tab keeps working against its original context (it's a live editor,
 * already built against `ctx.api`) — but [equals]/[hashCode] fold in the context's account and
 * repo, so a *new* [GiteaPRTimelineVirtualFile] built under a different context is never
 * considered "the same file" as a stale one. That, combined with
 * [GiteaPRToolWindowController][com.github.jpmand.idea.plugin.gitea.pullrequest.ui.toolwindow.GiteaPRToolWindowController]
 * proactively closing *every* open Conversation tab as soon as the context changes
 * (`closeAllTimelineEditors()`, called from `updateContent()` — mirrors the bundled GitHub
 * plugin's `GHPRFilesManagerImpl.closeAllFiles()` on disconnect) plus a same-PR safety net in
 * `openTimelineEditor()`, is what fixes "switching the active account doesn't update avatars in
 * the conversation" — previously, [FileEditorManager][com.intellij.openapi.fileEditor.FileEditorManager],
 * which tracks/reuses open editors by [VirtualFile][com.intellij.openapi.vfs.VirtualFile] identity,
 * could resolve "open the conversation for PR #N" to an editor built against the old context
 * (including the avatar loader in [GiteaPRTimelineFileEditor], constructed once from
 * `file.ctx.api`) because identity ignored the context entirely.
 *
 * The same identity gap still exists on
 * [GiteaPRDiffVirtualFile][com.github.jpmand.idea.plugin.gitea.pullrequest.diff.GiteaPRDiffVirtualFile]
 * (no [equals] override at all, so it falls back to identity — every open is a "new" file, which
 * avoids this bug but never reuses/focuses an already-open diff tab either).
 */
class GiteaPRTimelineVirtualFile(
    val prNumber: Int,
    val pr: GiteaPullRequest,
    val repository: GiteaPRRepository,
    val ctx: GiteaPRDataContext,
    private val project: Project,
) : LightVirtualFile("gitea-pr-$prNumber-timeline") {

    override fun isValid(): Boolean = !project.isDisposed
    override fun isWritable(): Boolean = false

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GiteaPRTimelineVirtualFile) return false
        return prNumber == other.prNumber &&
            project == other.project &&
            ctx.account.id == other.ctx.account.id &&
            ctx.repo.repositoryPath == other.ctx.repo.repositoryPath
    }

    override fun hashCode(): Int {
        var result = prNumber
        result = 31 * result + project.hashCode()
        result = 31 * result + ctx.account.id.hashCode()
        result = 31 * result + ctx.repo.repositoryPath.hashCode()
        return result
    }
}
