package com.github.jpmand.idea.plugin.gitea.api.rest.dto

import java.time.OffsetDateTime

/**
 * 
 * @param author
 * @param committer
 * @param created Created is the time when the commit was created
 * @param htmlUrl HTMLURL is the web URL for viewing this commit
 * @param message Message is the commit message
 * @param parents Parents contains parent commit metadata
 * @param sha SHA is the commit SHA hash
 * @param tree
 * @param url URL is the API URL for the commit
 */
data class FileCommitResponse(
    val author: CommitUser? = null,
    val committer: CommitUser? = null,
    /* Created is the time when the commit was created */
    val created: OffsetDateTime? = null,
    /* HTMLURL is the web URL for viewing this commit */
    val htmlUrl: String? = null,
    /* Message is the commit message */
    val message: String? = null,
    /* Parents contains parent commit metadata */
    val parents: Array<CommitMeta>? = null,
    /* SHA is the commit SHA hash */
    val sha: String? = null,
    val tree: CommitMeta? = null,
    /* URL is the API URL for the commit */
    val url: String? = null,
)

