package com.github.jpmand.idea.plugin.gitea.api.rest.dto


/**
 * EditIssueCommentOption options for editing a comment
 * @param body Body is the updated comment text content
 */
data class EditIssueCommentOption(
    /* Body is the updated comment text content */
    val body: String,
)

