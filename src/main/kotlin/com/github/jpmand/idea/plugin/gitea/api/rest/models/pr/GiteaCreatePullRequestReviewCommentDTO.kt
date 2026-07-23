package com.github.jpmand.idea.plugin.gitea.api.rest.models.pr

/** Inline diff comment included inside a create-review request body. */
open class GiteaCreatePullRequestReviewCommentDTO(
    val path: String,
    val body: String,
    /** 1-indexed line in the base file; 0 when commenting on an added (head-only) line. */
    val newPosition: Int = 0,
    val oldPosition: Int = 0
)

