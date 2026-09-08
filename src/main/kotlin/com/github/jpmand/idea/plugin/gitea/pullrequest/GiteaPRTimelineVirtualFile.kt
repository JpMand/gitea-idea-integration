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
