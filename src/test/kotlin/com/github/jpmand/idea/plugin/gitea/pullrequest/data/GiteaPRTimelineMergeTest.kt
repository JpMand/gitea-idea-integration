package com.github.jpmand.idea.plugin.gitea.pullrequest.data

import com.github.jpmand.idea.plugin.gitea.api.models.GiteaReview
import com.github.jpmand.idea.plugin.gitea.api.models.GiteaReviewState
import com.github.jpmand.idea.plugin.gitea.api.models.GiteaTimelineItem
import com.github.jpmand.idea.plugin.gitea.api.models.GiteaUser
import com.github.jpmand.idea.plugin.gitea.api.rest.dto.Commit
import com.github.jpmand.idea.plugin.gitea.api.rest.dto.RepoCommit
import com.github.jpmand.idea.plugin.gitea.api.rest.dto.TimelineComment
import com.github.jpmand.idea.plugin.gitea.api.rest.dto.User
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.OffsetDateTime
import java.util.Date

class GiteaPRTimelineMergeTest {

    private fun at(min: Int) = OffsetDateTime.parse("2026-01-01T00:00:00Z").plusMinutes(min.toLong())
    private fun user(login: String) = User(login = login)

    @Test
    fun `merges comments, events, reviews and commits in chronological order`() {
        val timeline = listOf(
            TimelineComment(id = 1, type = "comment", body = "first", user = user("alice"), createdAt = at(0)),
            TimelineComment(id = 2, type = "label", body = "1", user = user("bob"), createdAt = at(2)),
            TimelineComment(id = 3, type = "review", reviewId = 10, user = user("carol"), createdAt = at(4)),
            TimelineComment(id = 4, type = "code", reviewId = 10, body = "inline", user = user("carol"), createdAt = at(4)),
            TimelineComment(id = 5, type = "close", user = user("alice"), createdAt = at(6)),
        )
        val reviews = mapOf(
            10L to GiteaReview(
                id = 10, author = GiteaUser(1, "carol", null, null, null, null), body = "looks good",
                state = GiteaReviewState.APPROVED, submittedAt = Date.from(at(4).toInstant()),
                dismissed = false, stale = false, commitId = null, commentsCount = 0, htmlUrl = "",
            ),
        )
        val commits = listOf(
            Commit(sha = "abcdef1234567890", created = at(1), commit = RepoCommit(message = "do a thing\n\nbody")),
        )

        val items = mergeTimeline(timeline, reviews, emptyMap(), commits)

        assertEquals(
            listOf(
                GiteaTimelineItem.Comment::class,
                GiteaTimelineItem.Commit::class,
                GiteaTimelineItem.Event::class,   // label added
                GiteaTimelineItem.Review::class,
                GiteaTimelineItem.Event::class,   // closed
            ),
            items.map { it::class },
        )
        assertEquals("do a thing", (items[1] as GiteaTimelineItem.Commit).messageTitle)
        assertEquals(GiteaTimelineItem.Event.Kind.LABEL_ADDED, (items[2] as GiteaTimelineItem.Event).kind)
        assertEquals(GiteaReviewState.APPROVED, (items[3] as GiteaTimelineItem.Review).state)
    }

    @Test
    fun `a review surfaced as both review and comment rows is de-duplicated`() {
        val timeline = listOf(
            TimelineComment(id = 1, type = "review", reviewId = 7, user = user("x"), createdAt = at(0)),
            TimelineComment(id = 2, type = "comment", reviewId = 7, body = "b", user = user("x"), createdAt = at(0)),
        )
        val items = mergeTimeline(timeline, emptyMap(), emptyMap(), emptyList())
        assertEquals(1, items.count { it is GiteaTimelineItem.Review })
    }

    @Test
    fun `plain comments with empty body are dropped`() {
        val timeline = listOf(
            TimelineComment(id = 1, type = "comment", body = "  ", user = user("x"), createdAt = at(0)),
        )
        assertTrue(mergeTimeline(timeline, emptyMap(), emptyMap(), emptyList()).isEmpty())
    }
}
