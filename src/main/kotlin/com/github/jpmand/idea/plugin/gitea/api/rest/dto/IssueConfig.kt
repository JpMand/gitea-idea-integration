package com.github.jpmand.idea.plugin.gitea.api.rest.dto

/**
 * 
 * @param blankIssuesEnabled
 * @param contactLinks
 */
data class IssueConfig(
    val blankIssuesEnabled: Boolean? = null,
    val contactLinks: Array<IssueConfigContactLink>? = null,
)

