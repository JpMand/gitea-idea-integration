package com.github.jpmand.idea.plugin.gitea.api.models

/**
 * Domain-level representation of a Gitea pull request review state.
 * Mirrors [com.github.jpmand.idea.plugin.gitea.api.rest.models.pr.GiteaReviewStateEnum]
 * but lives in the domain layer so domain objects do not depend on the REST layer.
 */
enum class GiteaReviewState {
    APPROVED,
    REQUEST_CHANGES,
    COMMENT,
    PENDING,
    UNKNOWN
}

