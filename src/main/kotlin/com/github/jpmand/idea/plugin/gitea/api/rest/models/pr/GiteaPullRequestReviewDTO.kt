package com.github.jpmand.idea.plugin.gitea.api.rest.models.pr
import com.github.jpmand.idea.plugin.gitea.api.models.GiteaPullRequestReview
import com.github.jpmand.idea.plugin.gitea.api.models.GiteaReviewState
import com.github.jpmand.idea.plugin.gitea.api.rest.models.GiteaUserDTO
import java.util.Date
open class GiteaPullRequestReviewDTO(
    val id: Long,
    val user: GiteaUserDTO?,
    val body: String?,
    val state: GiteaReviewStateEnum?,
    val submittedAt: Date?,
    val stale: Boolean,
    val official: Boolean,
    val dismissed: Boolean,
    val commitId: String?
) {
    fun toReview(): GiteaPullRequestReview = GiteaPullRequestReview(
        id = id,
        author = user?.toUser() ?: throw IllegalStateException("Review user is null for review $id"),
        body = body,
        state = state?.toDomainState() ?: GiteaReviewState.UNKNOWN,
        submittedAt = submittedAt,
        dismissed = dismissed
    )
}
private fun GiteaReviewStateEnum.toDomainState(): GiteaReviewState = when (this) {
    GiteaReviewStateEnum.APPROVED -> GiteaReviewState.APPROVED
    GiteaReviewStateEnum.REQUEST_CHANGES -> GiteaReviewState.REQUEST_CHANGES
    GiteaReviewStateEnum.COMMENT -> GiteaReviewState.COMMENT
    GiteaReviewStateEnum.PENDING -> GiteaReviewState.PENDING
}
