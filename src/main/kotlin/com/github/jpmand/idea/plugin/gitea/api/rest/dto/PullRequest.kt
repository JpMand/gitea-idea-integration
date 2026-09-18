package com.github.jpmand.idea.plugin.gitea.api.rest.dto

import java.time.OffsetDateTime

/**
 * PullRequest represents a pull request
 * @param additions The number of lines added in the pull request
 * @param allowMaintainerEdit Whether maintainers can edit the pull request
 * @param assignee
 * @param assignees The list of users assigned to the pull request
 * @param base
 * @param body The description body of the pull request
 * @param changedFiles The number of files changed in the pull request
 * @param closedAt
 * @param comments The number of comments on the pull request
 * @param contentVersion The version of the pull request content for optimistic locking
 * @param createdAt
 * @param deletions The number of lines deleted in the pull request
 * @param diffUrl The URL to download the diff patch
 * @param draft Whether the pull request is a draft
 * @param dueDate
 * @param head
 * @param htmlUrl The HTML URL to view the pull request
 * @param id The unique identifier of the pull request
 * @param isLocked Whether the pull request conversation is locked
 * @param labels The labels attached to the pull request
 * @param mergeBase The merge base commit SHA
 * @param mergeCommitSha The SHA of the merge commit
 * @param mergeable Whether the pull request can be merged
 * @param merged Whether the pull request has been merged
 * @param mergedAt
 * @param mergedBy
 * @param milestone
 * @param number The pull request number
 * @param patchUrl The URL to download the patch file
 * @param pinOrder The pin order for the pull request
 * @param requestedReviewers The users requested to review the pull request
 * @param requestedReviewersTeams The teams requested to review the pull request
 * @param reviewComments number of review comments made on the diff of a PR review (not including comments on commits or issues in a PR)
 * @param state The current state of the pull request
 * @param title The title of the pull request
 * @param updatedAt
 * @param url The API URL of the pull request
 * @param user
 */
data class PullRequest(
    /* The number of lines added in the pull request */
    val additions: Long? = null,
    /* Whether maintainers can edit the pull request */
    val allowMaintainerEdit: Boolean? = null,
    val assignee: User? = null,
    /* The list of users assigned to the pull request */
    val assignees: Array<User>? = null,
    val base: PRBranchInfo? = null,
    /* The description body of the pull request */
    val body: String? = null,
    /* The number of files changed in the pull request */
    val changedFiles: Long? = null,
    val closedAt: OffsetDateTime? = null,
    /* The number of comments on the pull request */
    val comments: Long? = null,
    /* The version of the pull request content for optimistic locking */
    val contentVersion: Long? = null,
    val createdAt: OffsetDateTime? = null,
    /* The number of lines deleted in the pull request */
    val deletions: Long? = null,
    /* The URL to download the diff patch */
    val diffUrl: String? = null,
    /* Whether the pull request is a draft */
    val draft: Boolean? = null,
    val dueDate: OffsetDateTime? = null,
    val head: PRBranchInfo? = null,
    /* The HTML URL to view the pull request */
    val htmlUrl: String? = null,
    /* The unique identifier of the pull request */
    val id: Long? = null,
    /* Whether the pull request conversation is locked */
    val isLocked: Boolean? = null,
    /* The labels attached to the pull request */
    val labels: Array<Label>? = null,
    /* The merge base commit SHA */
    val mergeBase: String? = null,
    /* The SHA of the merge commit */
    val mergeCommitSha: String? = null,
    /* Whether the pull request can be merged */
    val mergeable: Boolean? = null,
    /* Whether the pull request has been merged */
    val merged: Boolean? = null,
    val mergedAt: OffsetDateTime? = null,
    val mergedBy: User? = null,
    val milestone: Milestone? = null,
    /* The pull request number */
    val number: Long? = null,
    /* The URL to download the patch file */
    val patchUrl: String? = null,
    /* The pin order for the pull request */
    val pinOrder: Long? = null,
    /* The users requested to review the pull request */
    val requestedReviewers: Array<User>? = null,
    /* The teams requested to review the pull request */
    val requestedReviewersTeams: Array<Team>? = null,
    /* number of review comments made on the diff of a PR review (not including comments on commits or issues in a PR) */
    val reviewComments: Long? = null,
    /* The current state of the pull request */
    val state: State? = null,
    /* The title of the pull request */
    val title: String? = null,
    val updatedAt: OffsetDateTime? = null,
    /* The API URL of the pull request */
    val url: String? = null,
    val user: User? = null,
) {


    /**
     * The current state of the pull request
     * Values: OPEN,CLOSED
     */
    enum class State(val value: String) {

        OPEN("open"),

        CLOSED("closed");

    }


}

