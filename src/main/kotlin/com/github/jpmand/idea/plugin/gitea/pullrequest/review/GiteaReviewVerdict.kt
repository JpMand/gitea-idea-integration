package com.github.jpmand.idea.plugin.gitea.pullrequest.review

import com.github.jpmand.idea.plugin.gitea.api.rest.dto.CreatePullReviewOptions
import com.github.jpmand.idea.plugin.gitea.api.rest.dto.SubmitPullReviewOptions

/** The outcome a review is submitted with, independent of whether it's a new or a pending review. */
enum class GiteaReviewVerdict(
    val createEvent: CreatePullReviewOptions.Event,
    /** Null for [PENDING] — an already-pending review can't be saved as pending again. */
    val submitEvent: SubmitPullReviewOptions.Event?,
) {
    COMMENT(CreatePullReviewOptions.Event.COMMENT, SubmitPullReviewOptions.Event.COMMENT),
    APPROVE(CreatePullReviewOptions.Event.APPROVED, SubmitPullReviewOptions.Event.APPROVED),
    REQUEST_CHANGES(CreatePullReviewOptions.Event.REQUESTCHANGES, SubmitPullReviewOptions.Event.REQUESTCHANGES),
    PENDING(CreatePullReviewOptions.Event.PENDING, null),
}

/**
 * The bundle key describing why Gitea would reject submitting a review with [verdict], [body] and
 * [commentCount] inline comments, or null when it's acceptable. Gitea answers both cases with a
 * 422: "review event REQUEST_CHANGES requires a body", and an empty COMMENT review.
 */
fun reviewSubmitProblem(verdict: GiteaReviewVerdict, body: String, commentCount: Int): String? = when {
    verdict == GiteaReviewVerdict.REQUEST_CHANGES && body.isBlank() -> "pull.request.review.submit.error.request.changes.body"
    verdict == GiteaReviewVerdict.COMMENT && body.isBlank() && commentCount == 0 -> "pull.request.review.submit.error.empty"
    else -> null
}
