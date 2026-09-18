package com.github.jpmand.idea.plugin.gitea.api.rest.dto


/**
 * RenameBranchRepoOption options when renaming a branch in a repository
 * @param name New branch name
 */
data class RenameBranchRepoOption(
    /* New branch name */
    val name: String,
)

