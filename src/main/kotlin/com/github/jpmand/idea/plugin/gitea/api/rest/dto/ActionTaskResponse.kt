package com.github.jpmand.idea.plugin.gitea.api.rest.dto

/**
 * ActionTaskResponse returns a ActionTask
 * @param totalCount TotalCount is the total number of workflow runs
 * @param workflowRuns Entries contains the list of workflow runs
 */
data class ActionTaskResponse(
    /* TotalCount is the total number of workflow runs */
    val totalCount: Long? = null,
    /* Entries contains the list of workflow runs */
    val workflowRuns: Array<ActionTask>? = null,
)

