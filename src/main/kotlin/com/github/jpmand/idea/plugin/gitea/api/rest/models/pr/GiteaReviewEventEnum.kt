package com.github.jpmand.idea.plugin.gitea.api.rest.models.pr

/**
 * Valid `event` values when submitting a review via `POST /repos/{owner}/{repo}/pulls/{index}/reviews`.
 *
 * Distinct from [GiteaReviewStateEnum] which covers all states returned by the server.
 */
enum class GiteaReviewEventEnum {
    APPROVED,
    REQUEST_CHANGES,
    COMMENT
}
