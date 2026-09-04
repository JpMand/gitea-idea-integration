package com.github.jpmand.idea.plugin.gitea.api.rest.dto


/**
 * RunDetails returns workflow_dispatch runid and url
 * @param htmlUrl
 * @param runUrl
 * @param workflowRunId
 */
data class RunDetails(
    val htmlUrl: String? = null,
    val runUrl: String? = null,
    val workflowRunId: Long? = null,
)

