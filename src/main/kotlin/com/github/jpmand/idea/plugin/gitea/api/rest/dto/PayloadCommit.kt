package com.github.jpmand.idea.plugin.gitea.api.rest.dto

import java.time.OffsetDateTime

/**
 * PayloadCommit represents a commit
 * @param added List of files added in this commit
 * @param author
 * @param committer
 * @param id sha1 hash of the commit
 * @param message The commit message
 * @param modified List of files modified in this commit
 * @param removed List of files removed in this commit
 * @param timestamp The timestamp when the commit was made
 * @param url The URL to view this commit
 * @param verification
 */
data class PayloadCommit(
    /* List of files added in this commit */
    val added: Array<String>? = null,
    val author: PayloadUser? = null,
    val committer: PayloadUser? = null,
    /* sha1 hash of the commit */
    val id: String? = null,
    /* The commit message */
    val message: String? = null,
    /* List of files modified in this commit */
    val modified: Array<String>? = null,
    /* List of files removed in this commit */
    val removed: Array<String>? = null,
    /* The timestamp when the commit was made */
    val timestamp: OffsetDateTime? = null,
    /* The URL to view this commit */
    val url: String? = null,
    val verification: PayloadCommitVerification? = null,
)

