package com.github.jpmand.idea.plugin.gitea.api.rest.dto

/**
 * WikiCommit page commit/revision
 * @param author
 * @param commiter
 * @param message The commit message
 * @param sha The commit SHA hash
 */
data class WikiCommit(
    val author: CommitUser? = null,
    val commiter: CommitUser? = null,
    /* The commit message */
    val message: String? = null,
    /* The commit SHA hash */
    val sha: String? = null,
)

