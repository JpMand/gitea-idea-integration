package com.github.jpmand.idea.plugin.gitea.api.rest.dto

import java.time.OffsetDateTime

/**
 * ActionWorkflowJob represents a WorkflowJob
 * @param completedAt
 * @param conclusion
 * @param createdAt
 * @param headBranch
 * @param headSha
 * @param htmlUrl
 * @param id
 * @param labels
 * @param name
 * @param runAttempt
 * @param runId
 * @param runUrl
 * @param runnerId
 * @param runnerName
 * @param startedAt
 * @param status
 * @param steps
 * @param url
 */
data class ActionWorkflowJob(
    val completedAt: OffsetDateTime? = null,
    val conclusion: String? = null,
    val createdAt: OffsetDateTime? = null,
    val headBranch: String? = null,
    val headSha: String? = null,
    val htmlUrl: String? = null,
    val id: Long? = null,
    val labels: Array<String>? = null,
    val name: String? = null,
    val runAttempt: Long? = null,
    val runId: Long? = null,
    val runUrl: String? = null,
    val runnerId: Long? = null,
    val runnerName: String? = null,
    val startedAt: OffsetDateTime? = null,
    val status: String? = null,
    val steps: Array<ActionWorkflowStep>? = null,
    val url: String? = null,
)

