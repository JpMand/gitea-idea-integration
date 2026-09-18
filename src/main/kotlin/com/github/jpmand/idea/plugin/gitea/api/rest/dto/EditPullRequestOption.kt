package com.github.jpmand.idea.plugin.gitea.api.rest.dto

import java.time.OffsetDateTime


/**
 * EditPullRequestOption options when modify pull request
 * @param allowMaintainerEdit Whether to allow maintainer edits
 * @param assignee The new primary assignee username
 * @param assignees The new list of assignee usernames
 * @param base The new base branch for the pull request
 * @param body The new description body for the pull request
 * @param contentVersion The current version of the pull request content to detect conflicts during editing
 * @param dueDate
 * @param labels The new list of label IDs for the pull request
 * @param milestone The new milestone ID for the pull request
 * @param state The new state for the pull request
 * @param title The new title for the pull request
 * @param unsetDueDate Whether to remove the current deadline
 */
data class EditPullRequestOption(
    /* Whether to allow maintainer edits */
    val allowMaintainerEdit: Boolean? = null,
    /* The new primary assignee username */
    val assignee: String? = null,
    /* The new list of assignee usernames */
    val assignees: Array<String>? = null,
    /* The new base branch for the pull request */
    val base: String? = null,
    /* The new description body for the pull request */
    val body: String? = null,
    /* The current version of the pull request content to detect conflicts during editing */
    val contentVersion: Long? = null,
    val dueDate: OffsetDateTime? = null,
    /* The new list of label IDs for the pull request */
    val labels: Array<Long>? = null,
    /* The new milestone ID for the pull request */
    val milestone: Long? = null,
    /* The new state for the pull request */
    val state: String? = null,
    /* The new title for the pull request */
    val title: String? = null,
    /* Whether to remove the current deadline */
    val unsetDueDate: Boolean? = null,
)

