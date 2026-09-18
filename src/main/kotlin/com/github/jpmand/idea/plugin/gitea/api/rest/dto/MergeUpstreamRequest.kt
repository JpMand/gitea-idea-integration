package com.github.jpmand.idea.plugin.gitea.api.rest.dto


/**
 * 
 * @param branch
 * @param ffOnly
 */
data class MergeUpstreamRequest(
    val branch: String? = null,
    val ffOnly: Boolean? = null,
)

