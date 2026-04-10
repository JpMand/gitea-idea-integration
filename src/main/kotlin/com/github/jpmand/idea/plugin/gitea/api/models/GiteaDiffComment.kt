package com.github.jpmand.idea.plugin.gitea.api.models

import java.util.Date

/**
 * Domain model for an inline diff review comment on a pull request.
 * Converted from [com.github.jpmand.idea.plugin.gitea.api.rest.models.pr.GiteaPullRequestReviewCommentDTO]
 * via [com.github.jpmand.idea.plugin.gitea.api.rest.models.pr.GiteaPullRequestReviewCommentDTO.toDiffComment].
 */
data class GiteaDiffComment(
    val id: Long,
    val author: GiteaUser,
    val body: String?,
    val path: String?,
    val line: Int?,
    val originalLine: Int?,
    val diffHunk: String?,
    val position: Int?,
    val originalPosition: Int?,
    val commitId: String?,
    val originalCommitId: String?,
    val inReplyToId: Long?,
    val createdAt: Date?,
    val updatedAt: Date?
)
