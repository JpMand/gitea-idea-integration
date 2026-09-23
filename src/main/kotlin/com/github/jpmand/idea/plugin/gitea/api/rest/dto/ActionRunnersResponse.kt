package com.github.jpmand.idea.plugin.gitea.api.rest.dto

/**
 * ActionRunnersResponse returns Runners
 * @param runners
 * @param totalCount
 */
data class ActionRunnersResponse(
    val runners: Array<ActionRunner>? = null,
    val totalCount: Long? = null,
)

