package com.github.jpmand.idea.plugin.gitea.pullrequest

import com.github.jpmand.idea.plugin.gitea.api.models.GiteaPullRequest
import com.github.jpmand.idea.plugin.gitea.pullrequest.data.GiteaPRRepository
import com.intellij.openapi.project.Project
import com.intellij.testFramework.LightVirtualFile

/**
 * Represents a single PR's details view as an editor tab (see `GiteaPRDetailsFileEditor`/
 * `GiteaPRDetailsEditorProvider`) — modeled on this plugin's own [com.github.jpmand.idea.plugin.gitea.pullrequest.diff.GiteaPRDiffVirtualFile]
 * pattern (a bare [LightVirtualFile]), not GitHub's heavier `ComplexPathVirtualFileWithoutContent`
 * (which needs a full custom VFS registration — disproportionate for a single, simple tab type).
 *
 * [repository] and [pr] are captured by value at the moment the PR was opened, not re-resolved
 * from the data-context holder later — if the account/repo context changes while this tab is
 * open, it keeps working against its original repository rather than tearing down, matching how
 * the existing diff-tab flow already behaves when the context changes underneath it.
 */
class GiteaPRDetailsVirtualFile(
    val prNumber: Int,
    val pr: GiteaPullRequest,
    val repository: GiteaPRRepository,
    private val project: Project,
) : LightVirtualFile("gitea-pr-$prNumber-details") {

    override fun isValid(): Boolean = !project.isDisposed
    override fun isWritable(): Boolean = false

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GiteaPRDetailsVirtualFile) return false
        return prNumber == other.prNumber && project == other.project
    }

    override fun hashCode(): Int = 31 * prNumber + project.hashCode()
}
