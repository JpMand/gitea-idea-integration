package com.github.jpmand.idea.plugin.gitea.api.rest.dto

import java.time.OffsetDateTime

/**
 * PullReviewComment represents a comment on a pull request review
 * @param body
 * @param commitId
 * @param createdAt
 * @param diffHunk
 * @param htmlUrl
 * @param id
 * @param originalCommitId
 * @param originalPosition
 * @param path
 * @param position
 * @param pullRequestReviewId
 * @param pullRequestUrl
 * @param resolver
 * @param updatedAt
 * @param user
 */
data class PullReviewComment(
    val body: String? = null,
    val commitId: String? = null,
    val createdAt: OffsetDateTime? = null,
    val diffHunk: String? = null,
    val htmlUrl: String? = null,
    val id: Long? = null,
    val originalCommitId: String? = null,
    val originalPosition: Int? = null,
    val path: String? = null,
    val position: Int? = null,
    val pullRequestReviewId: Long? = null,
    val pullRequestUrl: String? = null,
    val resolver: User? = null,
    val updatedAt: OffsetDateTime? = null,
    val user: User? = null,
)

