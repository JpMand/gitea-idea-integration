package com.github.jpmand.idea.plugin.gitea.api.models

import com.github.jpmand.idea.plugin.gitea.api.rest.dto.Issue
import com.github.jpmand.idea.plugin.gitea.api.rest.dto.TimelineComment
import com.github.jpmand.idea.plugin.gitea.api.rest.dto.User
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.OffsetDateTime

class GiteaTimelineEventTest {

    private fun at(min: Int) = OffsetDateTime.parse("2026-01-01T00:00:00Z").plusMinutes(min.toLong())
    private fun user(login: String) = User(login = login)
    private fun comment(type: String, block: TimelineComment.() -> TimelineComment = { this }) =
        TimelineComment(id = 1, type = type, user = user("alice"), createdAt = at(0)).block()

    @Test
    fun `the 4 reference kinds map from their distinct type strings`() {
        val referencer = Issue(number = 42, title = "Fix the thing")

        assertEquals(
            GiteaTimelineItem.Event.Kind.REFERENCED_FROM_ISSUE,
            (comment("issue_ref") { copy(refIssue = referencer) }.toTimelineItemOrNull() as GiteaTimelineItem.Event).kind,
        )
        assertEquals(
            "#42 Fix the thing",
            (comment("issue_ref") { copy(refIssue = referencer) }.toTimelineItemOrNull() as GiteaTimelineItem.Event).newValue,
        )
        assertEquals(
            GiteaTimelineItem.Event.Kind.REFERENCED_FROM_PULL_REQUEST,
            (comment("pull_ref") { copy(refIssue = referencer) }.toTimelineItemOrNull() as GiteaTimelineItem.Event).kind,
        )
        assertEquals(
            GiteaTimelineItem.Event.Kind.REFERENCED_FROM_COMMENT,
            (comment("comment_ref") { copy(refIssue = referencer) }.toTimelineItemOrNull() as GiteaTimelineItem.Event).kind,
        )
        val commitRef = comment("commit_ref") { copy(refCommitSha = "abcdef1234567890") }
            .toTimelineItemOrNull() as GiteaTimelineItem.Event
        assertEquals(GiteaTimelineItem.Event.Kind.REFERENCED_FROM_COMMIT, commitRef.kind)
        assertEquals("abcdef1", commitRef.newValue)
    }

    @Test
    fun `time-tracking and pin kinds map correctly`() {
        assertEquals(
            GiteaTimelineItem.Event.Kind.TIME_TRACKING_STARTED,
            (comment("start_tracking").toTimelineItemOrNull() as GiteaTimelineItem.Event).kind,
        )
        assertEquals(
            GiteaTimelineItem.Event.Kind.PINNED,
            (comment("pin").toTimelineItemOrNull() as GiteaTimelineItem.Event).kind,
        )
        assertEquals(
            GiteaTimelineItem.Event.Kind.UNPINNED,
            (comment("unpin").toTimelineItemOrNull() as GiteaTimelineItem.Event).kind,
        )
        assertEquals(
            GiteaTimelineItem.Event.Kind.AUTO_MERGE_SCHEDULED,
            (comment("pull_scheduled_merge").toTimelineItemOrNull() as GiteaTimelineItem.Event).kind,
        )
    }

    @Test
    fun `rows handled elsewhere or with no clean rendering are dropped`() {
        assertNull(comment("comment").toTimelineItemOrNull())
        assertNull(comment("code").toTimelineItemOrNull())
        assertNull(comment("review").toTimelineItemOrNull())
        assertNull(comment("pull_push").toTimelineItemOrNull())
        assertNull(comment("change_issue_ref").toTimelineItemOrNull())
        assertNull(comment("delete_time_manual").toTimelineItemOrNull())
    }
}
