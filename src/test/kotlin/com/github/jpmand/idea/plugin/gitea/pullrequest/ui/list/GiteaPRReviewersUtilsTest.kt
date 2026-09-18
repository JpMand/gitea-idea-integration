package com.github.jpmand.idea.plugin.gitea.pullrequest.ui.list

import com.github.jpmand.idea.plugin.gitea.api.models.GiteaReview
import com.github.jpmand.idea.plugin.gitea.api.models.GiteaReviewState
import com.github.jpmand.idea.plugin.gitea.api.models.GiteaUser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GiteaPRReviewersUtilsTest {

    private fun user(login: String) = GiteaUser(
        id = login.hashCode().toLong(),
        login = login,
        avatarUrl = null,
        email = null,
        fullName = null,
        htmlUrl = null,
    )

    private fun review(author: GiteaUser, state: GiteaReviewState) = GiteaReview(
        id = 1L,
        author = author,
        body = null,
        state = state,
        submittedAt = null,
        dismissed = false,
        stale = false,
        commitId = null,
        commentsCount = 0,
        htmlUrl = "",
    )

    @Test
    fun `no reviewers requested and no reviews yields empty map`() {
        val states = computeReviewerStates(requestedReviewers = emptyList(), reviews = emptyList())
        assertTrue(states.isEmpty())
    }

    @Test
    fun `requested reviewer with no review yet needs review`() {
        val bob = user("bob")
        val states = computeReviewerStates(requestedReviewers = listOf(bob), reviews = emptyList())
        assertEquals(GiteaPRReviewerState.NEEDS_REVIEW, states[bob])
    }

    @Test
    fun `approved review maps to approved`() {
        val alice = user("alice")
        val states = computeReviewerStates(
            requestedReviewers = emptyList(),
            reviews = listOf(review(alice, GiteaReviewState.APPROVED)),
        )
        assertEquals(GiteaPRReviewerState.APPROVED, states[alice])
    }

    @Test
    fun `request-changes review maps to changes requested`() {
        val alice = user("alice")
        val states = computeReviewerStates(
            requestedReviewers = emptyList(),
            reviews = listOf(review(alice, GiteaReviewState.REQUEST_CHANGES)),
        )
        assertEquals(GiteaPRReviewerState.CHANGES_REQUESTED, states[alice])
    }

    @Test
    fun `comment review maps to commented`() {
        val alice = user("alice")
        val states = computeReviewerStates(
            requestedReviewers = emptyList(),
            reviews = listOf(review(alice, GiteaReviewState.COMMENT)),
        )
        assertEquals(GiteaPRReviewerState.COMMENTED, states[alice])
    }

    @Test
    fun `pending and request-review states need review`() {
        val alice = user("alice")
        val bob = user("bob")
        val states = computeReviewerStates(
            requestedReviewers = emptyList(),
            reviews = listOf(review(alice, GiteaReviewState.PENDING), review(bob, GiteaReviewState.REQUEST_REVIEW)),
        )
        assertEquals(GiteaPRReviewerState.NEEDS_REVIEW, states[alice])
        assertEquals(GiteaPRReviewerState.NEEDS_REVIEW, states[bob])
    }

    @Test
    fun `latest review per author wins over earlier ones`() {
        val alice = user("alice")
        val states = computeReviewerStates(
            requestedReviewers = emptyList(),
            reviews = listOf(
                review(alice, GiteaReviewState.COMMENT),
                review(alice, GiteaReviewState.REQUEST_CHANGES),
                review(alice, GiteaReviewState.APPROVED),
            ),
        )
        assertEquals(GiteaPRReviewerState.APPROVED, states[alice])
    }

    @Test
    fun `a reviewer who already reviewed is not overridden by a stale requested-reviewer entry`() {
        val alice = user("alice")
        val states = computeReviewerStates(
            requestedReviewers = listOf(alice), // e.g. re-requested after addressing feedback
            reviews = listOf(review(alice, GiteaReviewState.APPROVED)),
        )
        assertEquals(GiteaPRReviewerState.APPROVED, states[alice])
    }

    @Test
    fun `sortedReviewerStates orders approved before changes-requested before commented before needs-review`() {
        val approved = user("approved")
        val changesRequested = user("changesRequested")
        val commented = user("commented")
        val needsReview = user("needsReview")
        val states = mapOf(
            needsReview to GiteaPRReviewerState.NEEDS_REVIEW,
            commented to GiteaPRReviewerState.COMMENTED,
            changesRequested to GiteaPRReviewerState.CHANGES_REQUESTED,
            approved to GiteaPRReviewerState.APPROVED,
        )
        val ordered = sortedReviewerStates(states).map { it.first }
        assertEquals(listOf(approved, changesRequested, commented, needsReview), ordered)
    }
}
