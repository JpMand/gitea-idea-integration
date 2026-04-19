package com.github.jpmand.idea.plugin.gitea.api.rest.models.pr

import com.github.jpmand.idea.plugin.gitea.api.models.GiteaReviewComment
import com.github.jpmand.idea.plugin.gitea.api.rest.models.GiteaUserDTO
import java.util.Date

open class GiteaPullRequestReviewCommentDTO(
    val id: Long,
    val user: GiteaUserDTO?,
    val body: String?,
    val createdAt: Date?,
    val updatedAt: Date?,
    val path: String?,
    val diffHunk: String?,
    val originalPosition: Int?,
    val position: Int?,
    val commitId: String?,
    val originalCommitId: String?,
    val pullRequestReviewId: Long?,
    val resolver: GiteaUserDTO?
) {
  fun toReviewComment(): GiteaReviewComment = GiteaReviewComment(
    id = id,
    author = user?.toUser(),
    body = body,
    createdAt = createdAt,
    updatedAt = updatedAt,
    path = path,
    newLine = position,
    oldLine = originalPosition,
    diffHunk = diffHunk,
    commitId = commitId,
    originalCommitId = originalCommitId,
    reviewId = pullRequestReviewId,
    resolver = resolver?.toUser()
  )
}

