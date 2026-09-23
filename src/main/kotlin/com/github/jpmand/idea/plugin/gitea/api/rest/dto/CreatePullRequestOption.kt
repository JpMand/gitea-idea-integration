package com.github.jpmand.idea.plugin.gitea.api.rest.dto

import java.time.OffsetDateTime


/**
 * CreatePullRequestOption options when creating a pull request
 * @param allowMaintainerEdit Whether maintainers can edit the pull request
 * @param assignee The primary assignee username
 * @param assignees The list of assignee usernames
 * @param base The base branch for the pull request
 * @param body The description body of the pull request
 * @param dueDate
 * @param head The head branch for the pull request, it could be a branch name on the base repository or a form like `<username>:<branch>` which refers to the user's fork repository's branch.
 * @param labels The list of label IDs to assign to the pull request
 * @param milestone The milestone ID to assign to the pull request
 * @param reviewers The list of reviewer usernames
 * @param teamReviewers The list of team reviewer names
 * @param title The title of the pull request
 */
data class CreatePullRequestOption(
    /* Whether maintainers can edit the pull request */
    val allowMaintainerEdit: Boolean? = null,
    /* The primary assignee username */
    val assignee: String? = null,
    /* The list of assignee usernames */
    val assignees: Array<String>? = null,
    /* The base branch for the pull request */
    val base: String? = null,
    /* The description body of the pull request */
    val body: String? = null,
    val dueDate: OffsetDateTime? = null,
    /* The head branch for the pull request, it could be a branch name on the base repository or a form like `<username>:<branch>` which refers to the user's fork repository's branch. */
    val head: String? = null,
    /* The list of label IDs to assign to the pull request */
    val labels: Array<Long>? = null,
    /* The milestone ID to assign to the pull request */
    val milestone: Long? = null,
    /* The list of reviewer usernames */
    val reviewers: Array<String>? = null,
    /* The list of team reviewer names */
    val teamReviewers: Array<String>? = null,
    /* The title of the pull request */
    val title: String? = null,
)

