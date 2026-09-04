package com.github.jpmand.idea.plugin.gitea.api.rest.dto

/**
 * 
 * @param author
 * @param committer
 * @param message Message is the commit message
 * @param tree
 * @param url URL is the API URL for the commit
 * @param verification
 */
data class RepoCommit(
    val author: CommitUser? = null,
    val committer: CommitUser? = null,
    /* Message is the commit message */
    val message: String? = null,
    val tree: CommitMeta? = null,
    /* URL is the API URL for the commit */
    val url: String? = null,
    val verification: PayloadCommitVerification? = null,
)

