package com.github.jpmand.idea.plugin.gitea.api.models

/** Domain-level review state, decoupled from the DTO deserialization enum. */
enum class GiteaReviewState {
    APPROVED,
    PENDING,
    COMMENT,
    REQUEST_CHANGES,
    REQUEST_REVIEW
}
