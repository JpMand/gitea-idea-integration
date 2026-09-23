package com.github.jpmand.idea.plugin.gitea.api.rest.dto

import java.time.OffsetDateTime


/**
 * EditIssueOption options for editing an issue
 * @param assignee deprecated
 * @param assignees
 * @param body
 * @param contentVersion The current version of the issue content to detect conflicts during editing
 * @param dueDate
 * @param milestone
 * @param projects list of project ids to set (replaces existing projects)
 * @param ref
 * @param state
 * @param title
 * @param unsetDueDate
 */
data class EditIssueOption(
    @Deprecated("use assignees instead")
    val assignee: String? = null,
    val assignees: Array<String>? = null,
    val body: String? = null,
    /* The current version of the issue content to detect conflicts during editing */
    val contentVersion: Long? = null,
    val dueDate: OffsetDateTime? = null,
    val milestone: Long? = null,
    /* list of project ids to set (replaces existing projects) */
    val projects: Array<Long>? = null,
    val ref: String? = null,
    val state: String? = null,
    val title: String? = null,
    val unsetDueDate: Boolean? = null,
)

