package com.github.jpmand.idea.plugin.gitea.api.rest.dto

import java.time.OffsetDateTime


/**
 * 
 * @param created Created is the time when the commit was created
 * @param sha SHA is the commit SHA hash
 * @param url URL is the API URL for the commit
 */
data class CommitMeta(
    /* Created is the time when the commit was created */
    val created: OffsetDateTime? = null,
    /* SHA is the commit SHA hash */
    val sha: String? = null,
    /* URL is the API URL for the commit */
    val url: String? = null,
)

