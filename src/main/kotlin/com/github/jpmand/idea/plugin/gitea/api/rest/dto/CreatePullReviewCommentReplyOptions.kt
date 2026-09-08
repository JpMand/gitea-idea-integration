package com.github.jpmand.idea.plugin.gitea.api.rest.dto


/**
 * CreatePullReviewCommentReplyOptions are options to reply to a pull request review comment
 * @param body
 */
data class CreatePullReviewCommentReplyOptions(
    val body: String? = null,
)

