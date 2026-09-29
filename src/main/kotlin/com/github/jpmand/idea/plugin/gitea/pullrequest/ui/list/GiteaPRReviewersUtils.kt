package com.github.jpmand.idea.plugin.gitea.pullrequest.ui.list

import com.github.jpmand.idea.plugin.gitea.api.models.GiteaReview
import com.github.jpmand.idea.plugin.gitea.api.models.GiteaReviewState
import com.github.jpmand.idea.plugin.gitea.api.models.GiteaUser
import com.intellij.collaboration.ui.codereview.details.data.ReviewState

/**
 * Domain-level reviewer state for list/details presentation, decoupled from [GiteaReviewState]
 * (which describes a single *review*, not a reviewer's overall status on a PR).
 */
enum class GiteaPRReviewerState {
    APPROVED,
    CHANGES_REQUESTED,
    COMMENTED,
    NEEDS_REVIEW,
}

/** Order in which reviewers are grouped/displayed — approved first. */
private val REVIEWER_DISPLAY_ORDER = listOf(
    GiteaPRReviewerState.APPROVED,
    GiteaPRReviewerState.CHANGES_REQUESTED,
    GiteaPRReviewerState.COMMENTED,
    GiteaPRReviewerState.NEEDS_REVIEW,
)

/**
 * Computes one review-state per reviewer, combining requested-but-not-yet-reviewed reviewers
 * with actual reviews (the latest review per author wins). Reviewers with no review yet — either
 * merely requested or otherwise absent from [reviews] — are [GiteaPRReviewerState.NEEDS_REVIEW].
 * The PR's author ([authorLogin]) is left out: replying to a thread leaves them a comment review,
 * and Gitea lists them among `requested_reviewers` afterwards, but they aren't a reviewer.
 */
fun computeReviewerStates(
    requestedReviewers: List<GiteaUser>,
    reviews: List<GiteaReview>,
    authorLogin: String? = null,
): Map<GiteaUser, GiteaPRReviewerState> {
    // Reviews are returned oldest-first; keep the last one seen per author as that author's current state.
    val latestByAuthor = LinkedHashMap<GiteaUser, GiteaReview>()
    for (review in reviews) {
        val author = review.author ?: continue
        latestByAuthor[author] = review
    }

    val result = LinkedHashMap<GiteaUser, GiteaPRReviewerState>()
    for ((author, review) in latestByAuthor) {
        result[author] = when (review.state) {
            GiteaReviewState.APPROVED -> GiteaPRReviewerState.APPROVED
            GiteaReviewState.REQUEST_CHANGES -> GiteaPRReviewerState.CHANGES_REQUESTED
            GiteaReviewState.COMMENT -> GiteaPRReviewerState.COMMENTED
            GiteaReviewState.PENDING, GiteaReviewState.REQUEST_REVIEW -> GiteaPRReviewerState.NEEDS_REVIEW
        }
    }
    for (reviewer in requestedReviewers) {
        result.putIfAbsent(reviewer, GiteaPRReviewerState.NEEDS_REVIEW)
    }
    if (authorLogin != null) result.keys.removeAll { it.login.equals(authorLogin, ignoreCase = true) }

    return result
}

/** The platform's review state for [this], which drives the reviewer avatar outline and status text. */
fun GiteaPRReviewerState.toReviewState(): ReviewState = when (this) {
    GiteaPRReviewerState.APPROVED -> ReviewState.ACCEPTED
    GiteaPRReviewerState.CHANGES_REQUESTED -> ReviewState.WAIT_FOR_UPDATES
    GiteaPRReviewerState.COMMENTED, GiteaPRReviewerState.NEEDS_REVIEW -> ReviewState.NEED_REVIEW
}

/** [computeReviewerStates]'s result, ordered approved-first for display. */
fun sortedReviewerStates(states: Map<GiteaUser, GiteaPRReviewerState>): List<Pair<GiteaUser, GiteaPRReviewerState>> =
    REVIEWER_DISPLAY_ORDER.flatMap { state -> states.filterValues { it == state }.map { it.key to it.value } }
