package com.github.jpmand.idea.plugin.gitea.api.rest.dto

/**
 * ActionWorkflowRunsResponse returns ActionWorkflowRuns
 * @param totalCount
 * @param workflowRuns
 */
data class ActionWorkflowRunsResponse(
    val totalCount: Long? = null,
    val workflowRuns: Array<ActionWorkflowRun>? = null,
)

