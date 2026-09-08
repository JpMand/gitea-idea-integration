package com.github.jpmand.idea.plugin.gitea.api.rest.dto


/**
 * CreateBranchRepoOption options when creating a branch in a repository
 * @param newBranchName Name of the branch to create
 * @param oldBranchName Name of the old branch to create from
 * @param oldRefName Name of the old branch/tag/commit to create from
 */
data class CreateBranchRepoOption(
    /* Name of the branch to create */
    val newBranchName: String,
    /* Name of the old branch to create from */
    val oldBranchName: String? = null,
    /* Name of the old branch/tag/commit to create from */
    val oldRefName: String? = null,
)

