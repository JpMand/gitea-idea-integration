package com.github.jpmand.idea.plugin.gitea.api.models

import com.github.jpmand.idea.plugin.gitea.api.rest.dto.PullReview
import java.util.Date

class GiteaReview(
    val id: Long,
    val author: GiteaUser?,
    val body: String?,
    val state: GiteaReviewState,
    val submittedAt: Date?,
    val dismissed: Boolean,
    val stale: Boolean,
    val commitId: String?,
    val commentsCount: Int,
    val htmlUrl : String
){
    companion object{
        fun fromDto(dto: PullReview): GiteaReview {
            return GiteaReview(
                id = dto.id ?: 0L,
                author = dto.user?.let { GiteaUser.fromDto(it) },
                body = dto.body,
                state = GiteaReviewState.fromDto(dto.state),
                submittedAt = dto.submittedAt?.toDate(),
                dismissed = dto.dismissed ?: false,
                stale = dto.stale ?: false,
                commitId = dto.commitId,
                commentsCount = dto.commentsCount?.toInt() ?: 0,
                htmlUrl = dto.htmlUrl ?: ""
            )
        }
    }
}
