package com.github.jpmand.idea.plugin.gitea.api.rest.dto

import java.time.OffsetDateTime


/**
 * AddTimeOption options for adding time to an issue
 * @param created
 * @param time time in seconds
 * @param userName username of the user who spent the time working on the issue (optional)
 */
data class AddTimeOption(
    val created: OffsetDateTime? = null,
    /* time in seconds */
    val time: Long,
    /* username of the user who spent the time working on the issue (optional) */
    val userName: String? = null,
)

