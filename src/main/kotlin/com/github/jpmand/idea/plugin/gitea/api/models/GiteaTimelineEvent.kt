package com.github.jpmand.idea.plugin.gitea.api.models

import com.github.jpmand.idea.plugin.gitea.api.rest.dto.TimelineComment

/**
 * Maps a raw Gitea [TimelineComment] "event" row to a [GiteaTimelineItem.Event], or null for row
 * types the timeline UI does not render.
 *
 * Covers every `CommentType` string Gitea's `/timeline` endpoint can emit (see
 * `commentStrings` in gitea's `models/issues/comment.go`), except:
 *  - `"comment"` — a plain conversation comment, handled by the aggregator, not here.
 *  - `"code"` — marks an inline (diff-line) review comment; those are rendered via the
 *    review/thread aggregation path, not the generic event pathway.
 *  - `"review"` — a submitted review; handled by the aggregator's dedicated `"review"` branch.
 *  - `"pull_push"` — new commits pushed; redundant with the commits endpoint, already rendered
 *    as [GiteaTimelineItem.Commit] rows.
 *  - `"change_issue_ref"`, `"delete_time_manual"` — obscure/rare rows with no clean single-value
 *    rendering; dropped rather than guessed at.
 */
fun TimelineComment.toTimelineItemOrNull(): GiteaTimelineItem? {
    val timestamp = createdAt?.toDate() ?: return null
    val actor = user?.let { GiteaUser.fromDto(it) }
    val id = id ?: 0L

    fun event(
        kind: GiteaTimelineItem.Event.Kind,
        oldValue: String? = oldTitle ?: oldRef,
        newValue: String? = newTitle ?: newRef,
    ) = GiteaTimelineItem.Event(
        id = id,
        actor = actor,
        timestamp = timestamp,
        kind = kind,
        label = label?.let { GiteaLabel.fromDto(it) },
        user = assignee?.let { GiteaUser.fromDto(it) },
        oldValue = oldValue,
        newValue = newValue,
    )

    /** "#<number> <title>" for the issue/PR that did the referencing, if known. */
    fun referencingIssueText(): String? {
        val issue = refIssue ?: return null
        val number = issue.number ?: return null
        return "#$number" + (issue.title?.let { " $it" } ?: "")
    }

    return when (type) {
        "close" -> event(GiteaTimelineItem.Event.Kind.CLOSED)
        "reopen" -> event(GiteaTimelineItem.Event.Kind.REOPENED)
        "merge_pull" -> event(GiteaTimelineItem.Event.Kind.MERGED)
        // Gitea encodes label add/remove in the row body: "1" == added, "" == removed.
        "label" -> event(
            if (body == "1") GiteaTimelineItem.Event.Kind.LABEL_ADDED
            else GiteaTimelineItem.Event.Kind.LABEL_REMOVED,
        )
        "milestone" -> event(GiteaTimelineItem.Event.Kind.MILESTONE_CHANGED)
        "assignees" -> event(
            if (removedAssignee == true) GiteaTimelineItem.Event.Kind.UNASSIGNED
            else GiteaTimelineItem.Event.Kind.ASSIGNED,
        )
        "review_request" -> event(
            if (removedAssignee == true) GiteaTimelineItem.Event.Kind.REVIEW_REQUEST_REMOVED
            else GiteaTimelineItem.Event.Kind.REVIEW_REQUESTED,
        )
        "dismiss_review" -> event(GiteaTimelineItem.Event.Kind.REVIEW_DISMISSED)
        "change_title" -> event(GiteaTimelineItem.Event.Kind.TITLE_CHANGED)
        "change_target_branch" -> event(GiteaTimelineItem.Event.Kind.BASE_BRANCH_CHANGED)
        "delete_branch" -> event(GiteaTimelineItem.Event.Kind.HEAD_BRANCH_DELETED)
        "lock" -> event(GiteaTimelineItem.Event.Kind.LOCKED)
        "unlock" -> event(GiteaTimelineItem.Event.Kind.UNLOCKED)

        // Gitea already records which of the 4 kinds this is via `type` itself.
        "issue_ref" -> event(GiteaTimelineItem.Event.Kind.REFERENCED_FROM_ISSUE, newValue = referencingIssueText())
        "pull_ref" -> event(GiteaTimelineItem.Event.Kind.REFERENCED_FROM_PULL_REQUEST, newValue = referencingIssueText())
        "comment_ref" -> event(GiteaTimelineItem.Event.Kind.REFERENCED_FROM_COMMENT, newValue = referencingIssueText())
        "commit_ref" -> event(GiteaTimelineItem.Event.Kind.REFERENCED_FROM_COMMIT, newValue = refCommitSha?.take(7))

        "start_tracking" -> event(GiteaTimelineItem.Event.Kind.TIME_TRACKING_STARTED)
        "stop_tracking" -> event(GiteaTimelineItem.Event.Kind.TIME_TRACKING_STOPPED)
        "add_time_manual" -> event(GiteaTimelineItem.Event.Kind.TIME_ADDED_MANUALLY)
        "cancel_tracking" -> event(GiteaTimelineItem.Event.Kind.TIME_TRACKING_CANCELLED)
        "change_time_estimate" -> event(GiteaTimelineItem.Event.Kind.TIME_ESTIMATE_CHANGED)
        "added_deadline" -> event(GiteaTimelineItem.Event.Kind.DUE_DATE_ADDED)
        "modified_deadline" -> event(GiteaTimelineItem.Event.Kind.DUE_DATE_MODIFIED)
        "removed_deadline" -> event(GiteaTimelineItem.Event.Kind.DUE_DATE_REMOVED)
        "add_dependency" -> event(GiteaTimelineItem.Event.Kind.DEPENDENCY_ADDED)
        "remove_dependency" -> event(GiteaTimelineItem.Event.Kind.DEPENDENCY_REMOVED)
        "project" -> event(GiteaTimelineItem.Event.Kind.PROJECT_CHANGED)
        "project_board" -> event(GiteaTimelineItem.Event.Kind.PROJECT_COLUMN_CHANGED)
        "pin" -> event(GiteaTimelineItem.Event.Kind.PINNED)
        "unpin" -> event(GiteaTimelineItem.Event.Kind.UNPINNED)
        "pull_scheduled_merge" -> event(GiteaTimelineItem.Event.Kind.AUTO_MERGE_SCHEDULED)
        "pull_cancel_scheduled_merge" -> event(GiteaTimelineItem.Event.Kind.AUTO_MERGE_CANCELLED)

        else -> null
    }
}
