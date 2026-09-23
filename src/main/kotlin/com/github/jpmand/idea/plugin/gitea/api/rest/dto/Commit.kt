package com.github.jpmand.idea.plugin.gitea.api.rest.dto

import java.time.OffsetDateTime

/**
 * 
 * @param author
 * @param commit
 * @param committer
 * @param created Created is the time when the commit was created
 * @param files Files contains information about files affected by the commit
 * @param htmlUrl HTMLURL is the web URL for viewing the commit
 * @param parents Parents contains the parent commit information
 * @param sha SHA is the commit SHA hash
 * @param stats
 * @param url URL is the API URL for the commit
 */
data class Commit(
    val author: User? = null,
    val commit: RepoCommit? = null,
    val committer: User? = null,
    /* Created is the time when the commit was created */
    val created: OffsetDateTime? = null,
    /* Files contains information about files affected by the commit */
    val files: Array<CommitAffectedFiles>? = null,
    /* HTMLURL is the web URL for viewing the commit */
    val htmlUrl: String? = null,
    /* Parents contains the parent commit information */
    val parents: Array<CommitMeta>? = null,
    /* SHA is the commit SHA hash */
    val sha: String? = null,
    val stats: CommitStats? = null,
    /* URL is the API URL for the commit */
    val url: String? = null,
)

