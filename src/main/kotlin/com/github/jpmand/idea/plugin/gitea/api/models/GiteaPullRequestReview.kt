package com.github.jpmand.idea.plugin.gitea.api.models

import java.util.Date

/**
 * Domain object representing a review on a Gitea pull request.
 * Converted from [com.github.jpmand.idea.plugin.gitea.api.rest.models.pr.GiteaPullRequestReviewDTO]
 * via [com.github.jpmand.idea.plugin.gitea.api.rest.models.pr.GiteaPullRequestReviewDTO.toReview].
 */
data class GiteaPullRequestReview(
    val id: Long,
    val author: GiteaUser,
    val body: String?,
    val state: GiteaReviewState,
    val submittedAt: Date?,
    val dismissed: Boolean
)

