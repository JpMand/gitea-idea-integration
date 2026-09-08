package com.github.jpmand.idea.plugin.gitea.api.rest.dto


/**
 * NewIssuePinsAllowed represents an API response that says if new Issue Pins are allowed
 * @param issues
 * @param pullRequests
 */
data class NewIssuePinsAllowed(
    val issues: Boolean? = null,
    val pullRequests: Boolean? = null,
)

