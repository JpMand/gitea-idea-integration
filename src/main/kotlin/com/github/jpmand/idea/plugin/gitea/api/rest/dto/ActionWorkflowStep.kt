package com.github.jpmand.idea.plugin.gitea.api.rest.dto

import java.time.OffsetDateTime


/**
 * ActionWorkflowStep represents a step of a WorkflowJob
 * @param completedAt
 * @param conclusion
 * @param name
 * @param number
 * @param startedAt
 * @param status
 */
data class ActionWorkflowStep(
    val completedAt: OffsetDateTime? = null,
    val conclusion: String? = null,
    val name: String? = null,
    val number: Long? = null,
    val startedAt: OffsetDateTime? = null,
    val status: String? = null,
)

