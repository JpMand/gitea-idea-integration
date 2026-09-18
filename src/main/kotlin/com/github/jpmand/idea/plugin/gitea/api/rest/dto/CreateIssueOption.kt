package com.github.jpmand.idea.plugin.gitea.api.rest.dto

import java.time.OffsetDateTime


/**
 * CreateIssueOption options to create one issue
 * @param assignee deprecated
 * @param assignees
 * @param body
 * @param closed
 * @param dueDate
 * @param labels list of label ids
 * @param milestone milestone id
 * @param projects list of project ids
 * @param ref
 * @param title
 */
data class CreateIssueOption(
    /* deprecated */
    @Deprecated("use assignees instead")
    val assignee: String? = null,
    val assignees: Array<String>? = null,
    val body: String? = null,
    val closed: Boolean? = null,
    val dueDate: OffsetDateTime? = null,
    /* list of label ids */
    val labels: Array<Long>? = null,
    /* milestone id */
    val milestone: Long? = null,
    /* list of project ids */
    val projects: Array<Long>? = null,
    val ref: String? = null,
    val title: String,
)

