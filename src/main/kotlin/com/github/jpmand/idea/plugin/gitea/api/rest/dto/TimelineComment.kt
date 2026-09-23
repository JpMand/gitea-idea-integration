package com.github.jpmand.idea.plugin.gitea.api.rest.dto

import java.time.OffsetDateTime

/**
 * TimelineComment represents a timeline comment (comment of any type) on a commit or issue
 * @param assignee
 * @param assigneeTeam
 * @param body Body contains the timeline event content
 * @param createdAt
 * @param dependentIssue
 * @param htmlUrl HTMLURL is the web URL for viewing the comment
 * @param id ID is the unique identifier for the timeline comment
 * @param issueUrl IssueURL is the API URL for the issue
 * @param label
 * @param milestone
 * @param newRef
 * @param newTitle
 * @param oldMilestone
 * @param oldProjectId
 * @param oldRef
 * @param oldTitle
 * @param projectId
 * @param pullRequestUrl PRURL is the API URL for the pull request (if applicable)
 * @param refAction
 * @param refComment
 * @param refCommitSha commit SHA where issue/PR was referenced
 * @param refIssue
 * @param removedAssignee whether the assignees were removed or added
 * @param resolveDoer
 * @param reviewId
 * @param trackedTime
 * @param type Type indicates the type of timeline event
 * @param updatedAt
 * @param user
 */
data class TimelineComment(
    val assignee: User? = null,
    val assigneeTeam: Team? = null,
    /* Body contains the timeline event content */
    val body: String? = null,
    val createdAt: OffsetDateTime? = null,
    val dependentIssue: Issue? = null,
    /* HTMLURL is the web URL for viewing the comment */
    val htmlUrl: String? = null,
    /* ID is the unique identifier for the timeline comment */
    val id: Long? = null,
    /* IssueURL is the API URL for the issue */
    val issueUrl: String? = null,
    val label: Label? = null,
    val milestone: Milestone? = null,
    val newRef: String? = null,
    val newTitle: String? = null,
    val oldMilestone: Milestone? = null,
    val oldProjectId: Long? = null,
    val oldRef: String? = null,
    val oldTitle: String? = null,
    val projectId: Long? = null,
    /* PRURL is the API URL for the pull request (if applicable) */
    val pullRequestUrl: String? = null,
    val refAction: String? = null,
    val refComment: Comment? = null,
    /* commit SHA where issue/PR was referenced */
    val refCommitSha: String? = null,
    val refIssue: Issue? = null,
    /* whether the assignees were removed or added */
    val removedAssignee: Boolean? = null,
    val resolveDoer: User? = null,
    val reviewId: Long? = null,
    val trackedTime: TrackedTime? = null,
    /* Type indicates the type of timeline event */
    val type: String? = null,
    val updatedAt: OffsetDateTime? = null,
    val user: User? = null,
)

