package com.github.jpmand.idea.plugin.gitea.api.rest.models.pr
import com.github.jpmand.idea.plugin.gitea.api.models.GiteaReview
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
  fun toReview(): GiteaReview = GiteaReview(
    id = id,
    author = user?.toUser(),
    body = body,
    state = when (state) {
      GiteaReviewStateEnum.APPROVED -> GiteaReviewState.APPROVED
      GiteaReviewStateEnum.REQUEST_CHANGES -> GiteaReviewState.REQUEST_CHANGES
      GiteaReviewStateEnum.COMMENT -> GiteaReviewState.COMMENT
      GiteaReviewStateEnum.REQUEST_REVIEW -> GiteaReviewState.REQUEST_REVIEW
      GiteaReviewStateEnum.PENDING, null -> GiteaReviewState.PENDING
    },
    submittedAt = submittedAt,
    dismissed = dismissed,
    stale = stale,
    commitId = commitId
  )
}