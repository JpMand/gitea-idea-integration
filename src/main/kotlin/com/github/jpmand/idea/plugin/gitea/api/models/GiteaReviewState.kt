package com.github.jpmand.idea.plugin.gitea.api.models

import com.github.jpmand.idea.plugin.gitea.api.rest.dto.PullReview

/** Domain-level review state, decoupled from the DTO deserialization enum. */
enum class GiteaReviewState {
    APPROVED,
    PENDING,
    COMMENT,
    REQUEST_CHANGES,
    REQUEST_REVIEW;

    companion object {
        fun fromDto(state: PullReview.State?): GiteaReviewState = when (state) {
            PullReview.State.APPROVED -> APPROVED
            PullReview.State.COMMENT -> COMMENT
            PullReview.State.REQUESTCHANGES -> REQUEST_CHANGES
            PullReview.State.REQUESTREVIEW -> REQUEST_REVIEW
            PullReview.State.PENDING, null -> PENDING
        }
    }
}
