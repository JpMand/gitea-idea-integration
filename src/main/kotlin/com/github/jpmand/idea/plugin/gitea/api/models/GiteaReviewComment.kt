package com.github.jpmand.idea.plugin.gitea.api.models

import com.github.jpmand.idea.plugin.gitea.api.rest.dto.PullReviewComment
import java.util.Date

data class GiteaReviewComment(
    val id: Long,
    val author: GiteaUser?,
    val body: String?,
    val createdAt: Date?,
    val updatedAt: Date?,
    val path: String?,
    /** 1-indexed line number in the head (new) file. Null for base-only deleted-line comments. */
    val newLine: Int?,
    /** 1-indexed line number in the base (old) file. Null for head-only added-line comments. */
    val oldLine: Int?,
    val diffHunk: String?,
    val commitId: String?,
    val originalCommitId: String?,
    val reviewId: Long?,
    /** Non-null user means this comment has been resolved. */
    val resolver: GiteaUser?
) {
    val isResolved: Boolean get() = resolver != null

    companion object {
        fun fromDto(dto: PullReviewComment): GiteaReviewComment = GiteaReviewComment(
            id = dto.id ?: 0L,
            author = dto.user?.let { GiteaUser.fromDto(it) },
            body = dto.body,
            createdAt = dto.createdAt?.toDate(),
            updatedAt = dto.updatedAt?.toDate(),
            path = dto.path,
            newLine = dto.position,
            oldLine = dto.originalPosition,
            diffHunk = dto.diffHunk,
            commitId = dto.commitId,
            originalCommitId = dto.originalCommitId,
            reviewId = dto.pullRequestReviewId,
            resolver = dto.resolver?.let { GiteaUser.fromDto(it) },
        )
    }
}
