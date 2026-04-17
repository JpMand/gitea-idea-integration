package com.github.jpmand.idea.plugin.gitea.api.rest.models.pr

/**
 * POST body for `POST /repos/{owner}/{repo}/pulls/{index}/reviews`.
 * [event] controls the review outcome — one of [GiteaReviewEventEnum].
 */
open class GiteaCreatePullRequestReviewRequestDTO(
    val body: String?,
    val commitId: String?,
    val event: GiteaReviewEventEnum?,
    val comments: List<GiteaCreatePullRequestReviewCommentDTO>?
)

