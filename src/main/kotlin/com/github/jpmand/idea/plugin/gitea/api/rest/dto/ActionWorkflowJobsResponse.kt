package com.github.jpmand.idea.plugin.gitea.api.rest.dto

/**
 * ActionWorkflowJobsResponse returns ActionWorkflowJobs
 * @param jobs
 * @param totalCount
 */
data class ActionWorkflowJobsResponse(
    val jobs: Array<ActionWorkflowJob>? = null,
    val totalCount: Long? = null,
)

