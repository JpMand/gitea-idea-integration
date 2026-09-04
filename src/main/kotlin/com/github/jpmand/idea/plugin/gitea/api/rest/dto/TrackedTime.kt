package com.github.jpmand.idea.plugin.gitea.api.rest.dto

import java.time.OffsetDateTime

/**
 * TrackedTime worked time for an issue / pr
 * @param created
 * @param id ID is the unique identifier for the tracked time entry
 * @param issue
 * @param issueId deprecated (only for backwards compatibility)
 * @param time Time in seconds
 * @param userId deprecated (only for backwards compatibility)
 * @param userName username of the user
 */
data class TrackedTime(
    val created: OffsetDateTime? = null,
    /* ID is the unique identifier for the tracked time entry */
    val id: Long? = null,
    val issue: Issue? = null,
    /* deprecated (only for backwards compatibility) */
    val issueId: Long? = null,
    /* Time in seconds */
    val time: Long? = null,
    /* deprecated (only for backwards compatibility) */
    val userId: Long? = null,
    /* username of the user */
    val userName: String? = null,
)

