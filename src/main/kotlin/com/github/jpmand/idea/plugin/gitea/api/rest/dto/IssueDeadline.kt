package com.github.jpmand.idea.plugin.gitea.api.rest.dto

import java.time.OffsetDateTime


/**
 * IssueDeadline represents an issue deadline
 * @param dueDate
 */
data class IssueDeadline(
    val dueDate: OffsetDateTime? = null,
)

