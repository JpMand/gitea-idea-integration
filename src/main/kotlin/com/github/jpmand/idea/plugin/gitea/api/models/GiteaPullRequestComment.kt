package com.github.jpmand.idea.plugin.gitea.api.models

import java.util.Date

/**
 * Unified domain comment model covering both issue-level PR comments and
 * inline review diff comments. Inline-only fields ([path], [diffHunk], [position])
 * are null for issue-level comments.
 *
 * Converted from:
 * - [com.github.jpmand.idea.plugin.gitea.api.rest.models.pr.GiteaIssueCommentDTO.toComment]
 * - [com.github.jpmand.idea.plugin.gitea.api.rest.models.pr.GiteaPullRequestReviewCommentDTO.toComment]
 */
data class GiteaPullRequestComment(
    val id: Long,
    val author: GiteaUser,
    val body: String?,
    val createdAt: Date?,
    val updatedAt: Date?,
    // Inline-only fields (null for issue-level comments)
    val path: String? = null,
    val diffHunk: String? = null,
    val position: Int? = null
)

