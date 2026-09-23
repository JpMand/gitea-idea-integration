package com.github.jpmand.idea.plugin.gitea.api.rest.dto

/**
 * IssueTemplate represents an issue template for a repository
 * @param about
 * @param assignees
 * @param body
 * @param content
 * @param fileName
 * @param labels
 * @param name
 * @param ref
 * @param title
 */
data class IssueTemplate(
    val about: String? = null,
    val assignees: IssueTemplateStringSlice? = null,
    val body: Array<IssueFormField>? = null,
    val content: String? = null,
    val fileName: String? = null,
    val labels: IssueTemplateStringSlice? = null,
    val name: String? = null,
    val ref: String? = null,
    val title: String? = null,
)

