package com.github.jpmand.idea.plugin.gitea.api.rest.dto

/**
 * UserHeatmapData represents the data needed to create a heatmap
 * @param contributions
 * @param timestamp
 */
data class UserHeatmapData(
    val contributions: Long? = null,
    val timestamp: TimeStamp? = null,
)

