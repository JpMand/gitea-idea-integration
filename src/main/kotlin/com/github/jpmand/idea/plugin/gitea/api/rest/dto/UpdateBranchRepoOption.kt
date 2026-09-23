package com.github.jpmand.idea.plugin.gitea.api.rest.dto


/**
 * UpdateBranchRepoOption options when updating a branch reference in a repository
 * @param force Force update even if the change is not a fast-forward
 * @param newCommitId New commit SHA (or any ref) the branch should point to
 * @param oldCommitId Expected old commit SHA of the branch; if provided it must match the current tip
 */
data class UpdateBranchRepoOption(
    /* Force update even if the change is not a fast-forward */
    val force: Boolean? = null,
    /* New commit SHA (or any ref) the branch should point to */
    val newCommitId: String,
    /* Expected old commit SHA of the branch; if provided it must match the current tip */
    val oldCommitId: String? = null,
)

