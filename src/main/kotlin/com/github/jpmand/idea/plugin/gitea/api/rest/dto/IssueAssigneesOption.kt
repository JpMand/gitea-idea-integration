package com.github.jpmand.idea.plugin.gitea.api.rest.dto


/**
 * IssueAssigneesOption options for adding/removing issue assignees
 * @param assignees
 */
data class IssueAssigneesOption(
    val assignees: Array<String>? = null,
)

