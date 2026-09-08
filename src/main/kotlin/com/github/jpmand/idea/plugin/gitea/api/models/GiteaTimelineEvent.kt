package com.github.jpmand.idea.plugin.gitea.api.models

import com.github.jpmand.idea.plugin.gitea.api.rest.dto.TimelineComment

/**
 * Maps a raw Gitea [TimelineComment] "event" row to a [GiteaTimelineItem.Event], or null for row
 * types the timeline UI does not render (comments, reviews and commits are handled by the
 * aggregator, not here; project/time-tracking/dependency events are dropped for Milestone 1).
 */
fun TimelineComment.toTimelineItemOrNull(): GiteaTimelineItem? {
    val timestamp = createdAt?.toDate() ?: return null
    val actor = user?.let { GiteaUser.fromDto(it) }
    val id = id ?: 0L

    fun event(kind: GiteaTimelineItem.Event.Kind) = GiteaTimelineItem.Event(
        id = id,
        actor = actor,
        timestamp = timestamp,
        kind = kind,
        label = label?.let { GiteaLabel.fromDto(it) },
        user = assignee?.let { GiteaUser.fromDto(it) },
        oldValue = oldTitle ?: oldRef,
        newValue = newTitle ?: newRef,
    )

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
        "change_target_branch", "change_branch" -> event(GiteaTimelineItem.Event.Kind.BASE_BRANCH_CHANGED)
        "delete_branch" -> event(GiteaTimelineItem.Event.Kind.HEAD_BRANCH_DELETED)
        "lock" -> event(GiteaTimelineItem.Event.Kind.LOCKED)
        "unlock" -> event(GiteaTimelineItem.Event.Kind.UNLOCKED)
        "issue_ref", "pull_ref", "comment_ref" -> event(GiteaTimelineItem.Event.Kind.REFERENCED)
        else -> null
    }
}
