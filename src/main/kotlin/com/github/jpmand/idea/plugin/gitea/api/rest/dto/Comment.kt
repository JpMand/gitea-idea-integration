package com.github.jpmand.idea.plugin.gitea.api.rest.dto

import java.time.OffsetDateTime

/**
 * Comment represents a comment on a commit or issue
 * @param assets Attachments contains files attached to the comment
 * @param body Body contains the comment text content
 * @param createdAt
 * @param htmlUrl HTMLURL is the web URL for viewing the comment
 * @param id ID is the unique identifier for the comment
 * @param issueUrl IssueURL is the API URL for the issue
 * @param originalAuthor OriginalAuthor is the original author name (for imported comments)
 * @param originalAuthorId OriginalAuthorID is the original author ID (for imported comments)
 * @param pullRequestUrl PRURL is the API URL for the pull request (if applicable)
 * @param updatedAt
 * @param user
 */
data class Comment(
    /* Attachments contains files attached to the comment */
    val assets: Array<Attachment>? = null,
    /* Body contains the comment text content */
    val body: String? = null,
    val createdAt: OffsetDateTime? = null,
    /* HTMLURL is the web URL for viewing the comment */
    val htmlUrl: String? = null,
    /* ID is the unique identifier for the comment */
    val id: Long? = null,
    /* IssueURL is the API URL for the issue */
    val issueUrl: String? = null,
    /* OriginalAuthor is the original author name (for imported comments) */
    val originalAuthor: String? = null,
    /* OriginalAuthorID is the original author ID (for imported comments) */
    val originalAuthorId: Long? = null,
    /* PRURL is the API URL for the pull request (if applicable) */
    val pullRequestUrl: String? = null,
    val updatedAt: OffsetDateTime? = null,
    val user: User? = null,
)

