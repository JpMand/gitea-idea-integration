package com.github.jpmand.idea.plugin.gitea.api.rest.dto


/**
 * CreateIssueCommentOption options for creating a comment on an issue
 * @param body Body is the comment text content
 */
data class CreateIssueCommentOption(
    /* Body is the comment text content */
    val body: String,
)

