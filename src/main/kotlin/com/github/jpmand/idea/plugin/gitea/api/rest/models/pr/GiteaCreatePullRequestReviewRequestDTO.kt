package com.github.jpmand.idea.plugin.gitea.api.rest.models.pr

/**
 * POST body for `POST /repos/{owner}/{repo}/pulls/{index}/reviews`.
 * [event] controls the review outcome — use [GiteaReviewStateEnum.APPROVED],
 * [GiteaReviewStateEnum.REQUEST_CHANGES], or [GiteaReviewStateEnum.COMMENT].
 */
open class GiteaCreatePullRequestReviewRequestDTO(
    val body: String?,
    val commitId: String?,
    val event: GiteaReviewStateEnum?,
    val comments: List<GiteaCreatePullRequestReviewCommentDTO>?
)

