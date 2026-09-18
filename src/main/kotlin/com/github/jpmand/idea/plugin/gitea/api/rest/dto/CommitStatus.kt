package com.github.jpmand.idea.plugin.gitea.api.rest.dto

import java.time.OffsetDateTime

/**
 * CommitStatus holds a single status of a single Commit
 * @param context Context is the unique context identifier for the status
 * @param createdAt
 * @param creator
 * @param description Description provides a brief description of the status
 * @param id ID is the unique identifier for the commit status
 * @param status State represents the status state (pending, success, error, failure)
 * @param targetUrl TargetURL is the URL to link to for more details
 * @param updatedAt
 * @param url URL is the API URL for this status
 */
data class CommitStatus(
    /* Context is the unique context identifier for the status */
    val context: String? = null,
    val createdAt: OffsetDateTime? = null,
    val creator: User? = null,
    /* Description provides a brief description of the status */
    val description: String? = null,
    /* ID is the unique identifier for the commit status */
    val id: Long? = null,
    /* State represents the status state (pending, success, error, failure) */
    val status: Status? = null,
    /* TargetURL is the URL to link to for more details */
    val targetUrl: String? = null,
    val updatedAt: OffsetDateTime? = null,
    /* URL is the API URL for this status */
    val url: String? = null,
) {


    /**
     * State represents the status state (pending, success, error, failure)
     * Values: PENDING,SUCCESS,ERROR,FAILURE,WARNING,SKIPPED
     */
    enum class Status(val value: String) {

        PENDING("pending"),

        SUCCESS("success"),

        ERROR("error"),

        FAILURE("failure"),

        WARNING("warning"),

        SKIPPED("skipped");

    }


}

