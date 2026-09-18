package com.github.jpmand.idea.plugin.gitea.api.rest.dto

/**
 * ActionArtifactsResponse returns ActionArtifacts
 * @param artifacts
 * @param totalCount
 */
data class ActionArtifactsResponse(
    val artifacts: Array<ActionArtifact>? = null,
    val totalCount: Long? = null,
)

