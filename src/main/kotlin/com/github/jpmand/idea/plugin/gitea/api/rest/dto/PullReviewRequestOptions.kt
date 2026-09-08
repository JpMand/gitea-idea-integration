package com.github.jpmand.idea.plugin.gitea.api.rest.dto


/**
 * PullReviewRequestOptions are options to add or remove pull request review requests
 * @param reviewers
 * @param teamReviewers
 */
data class PullReviewRequestOptions(
    val reviewers: Array<String>? = null,
    val teamReviewers: Array<String>? = null,
)

