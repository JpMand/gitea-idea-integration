package com.github.jpmand.idea.plugin.gitea.api.models

import java.util.Date

/**
 * One entry in a PR's activity timeline (Conversation). Built by
 * [com.github.jpmand.idea.plugin.gitea.pullrequest.data.mergeTimeline] from the Gitea
 * `issues/{index}/timeline` endpoint plus the reviews / commits endpoints.
 */
sealed interface GiteaTimelineItem {
    val id: Long
    val actor: GiteaUser?
    val timestamp: Date

    /** A plain conversation comment. */
    data class Comment(
        override val id: Long,
        override val actor: GiteaUser?,
        override val timestamp: Date,
        val body: String?,
        val htmlUrl: String?,
    ) : GiteaTimelineItem

    /** A commit pushed to the PR branch. */
    data class Commit(
        override val id: Long,
        override val actor: GiteaUser?,
        override val timestamp: Date,
        val sha: String,
        val shortSha: String,
        val messageTitle: String,
        val htmlUrl: String?,
    ) : GiteaTimelineItem

    /** A submitted review, with its inline comment threads. */
    data class Review(
        override val id: Long,
        override val actor: GiteaUser?,
        override val timestamp: Date,
        val state: GiteaReviewState,
        val body: String?,
        val htmlUrl: String?,
        val threads: List<GiteaReviewThread>,
    ) : GiteaTimelineItem

    /** A metadata change (label added/removed, status change, assignee change, …). */
    data class Event(
        override val id: Long,
        override val actor: GiteaUser?,
        override val timestamp: Date,
        val kind: Kind,
        val label: GiteaLabel? = null,
        val user: GiteaUser? = null,
        val oldValue: String? = null,
        val newValue: String? = null,
    ) : GiteaTimelineItem {

        enum class Kind {
            CLOSED,
            REOPENED,
            MERGED,
            LABEL_ADDED,
            LABEL_REMOVED,
            MILESTONE_CHANGED,
            ASSIGNED,
            UNASSIGNED,
            REVIEW_REQUESTED,
            REVIEW_REQUEST_REMOVED,
            REVIEW_DISMISSED,
            TITLE_CHANGED,
            BASE_BRANCH_CHANGED,
            HEAD_BRANCH_DELETED,
            LOCKED,
            UNLOCKED,

            /** This PR was referenced from a plain issue's body/comment. [newValue] carries the
             * referencing issue's "#number title" for display. */
            REFERENCED_FROM_ISSUE,
            /** This PR was referenced from another pull request's body/comment. [newValue]
             * carries the referencing PR's "#number title". */
            REFERENCED_FROM_PULL_REQUEST,
            /** This PR was referenced from within a comment (on this or another issue/PR).
             * [newValue] carries a short description, if any. */
            REFERENCED_FROM_COMMENT,
            /** This PR was referenced from a commit message. [newValue] carries the short SHA. */
            REFERENCED_FROM_COMMIT,

            TIME_TRACKING_STARTED,
            TIME_TRACKING_STOPPED,
            TIME_ADDED_MANUALLY,
            TIME_TRACKING_CANCELLED,
            TIME_ESTIMATE_CHANGED,
            DUE_DATE_ADDED,
            DUE_DATE_MODIFIED,
            DUE_DATE_REMOVED,
            DEPENDENCY_ADDED,
            DEPENDENCY_REMOVED,
            PROJECT_CHANGED,
            PROJECT_COLUMN_CHANGED,
            PINNED,
            UNPINNED,
            AUTO_MERGE_SCHEDULED,
            AUTO_MERGE_CANCELLED,
        }
    }
}
