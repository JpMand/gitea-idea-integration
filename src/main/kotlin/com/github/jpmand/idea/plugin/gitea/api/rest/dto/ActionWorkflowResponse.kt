package com.github.jpmand.idea.plugin.gitea.api.rest.dto

/**
 * ActionWorkflowResponse returns a ActionWorkflow
 * @param totalCount
 * @param workflows
 */
data class ActionWorkflowResponse(
    val totalCount: Long? = null,
    val workflows: Array<ActionWorkflow>? = null,
)

