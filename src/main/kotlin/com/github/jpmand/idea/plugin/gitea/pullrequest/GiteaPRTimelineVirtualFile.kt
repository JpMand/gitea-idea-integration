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
 * this tab is open it keeps working against its original repository.
 *
 * KNOWN ISSUE (tracked for the write-actions phase): [equals]/[hashCode] key only on [prNumber] +
 * [project], not on [ctx]. Since [FileEditorManager][com.intellij.openapi.fileEditor.FileEditorManager]
 * tracks/reuses open editors by [VirtualFile][com.intellij.openapi.vfs.VirtualFile] identity, opening
 * "the same" PR's conversation again after switching the active Gitea account can resolve to the
 * previously-open tab/editor — built against the *old* [ctx] (including the avatar loader in
 * [GiteaPRTimelineFileEditor], which is constructed once from `file.ctx.api`). This is the likely
 * cause of "switching accounts doesn't update avatars in the conversation, even after closing and
 * reopening the tab": a genuinely fresh [GiteaPRTimelineVirtualFile] with the new [ctx] can still
 * `equals()` an editor the platform hasn't actually discarded. Fix candidates: fold [ctx] (or at
 * least [ctx]'s account id) into equality/hashCode so an account switch opens a distinct tab; and/or
 * have [GiteaPRDataContextHolder][com.github.jpmand.idea.plugin.gitea.pullrequest.data.GiteaPRDataContextHolder]
 * proactively close any open PR editors when its context changes. The same identity gap exists on
 * [com.github.jpmand.idea.plugin.gitea.pullrequest.diff.GiteaPRDiffVirtualFile] (no [equals] override
 * at all — falls back to [LightVirtualFile]'s default).
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
        return prNumber == other.prNumber && project == other.project
    }

    override fun hashCode(): Int = 31 * prNumber + project.hashCode()
}
