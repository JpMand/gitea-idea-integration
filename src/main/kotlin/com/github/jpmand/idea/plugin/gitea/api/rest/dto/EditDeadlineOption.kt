package com.github.jpmand.idea.plugin.gitea.api.rest.dto

import java.time.OffsetDateTime


/**
 * EditDeadlineOption options for creating a deadline
 * @param dueDate
 */
data class EditDeadlineOption(
    val dueDate: OffsetDateTime,
)

