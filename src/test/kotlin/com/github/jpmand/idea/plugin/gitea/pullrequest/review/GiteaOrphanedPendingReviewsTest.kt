package com.github.jpmand.idea.plugin.gitea.pullrequest.review

import com.github.jpmand.idea.plugin.gitea.api.models.GiteaReview
import com.github.jpmand.idea.plugin.gitea.api.models.GiteaReviewState
import org.junit.Assert.assertEquals
import org.junit.Test

class GiteaOrphanedPendingReviewsTest {

  private fun pending(id: Long, comments: Int = 0, body: String? = null) =
    GiteaReview(id, null, body, GiteaReviewState.PENDING, null, false, false, null, comments, "")

  @Test
  fun `an empty review created by the failed request is orphaned`() {
    assertEquals(listOf(7L), orphanedPendingReviews(listOf(pending(7)), existingReviewIds = emptySet()).map { it.id })
  }

  @Test
  fun `a pending review that existed before the request is kept even when empty`() {
    val reviews = listOf(pending(3), pending(7))
    assertEquals(listOf(7L), orphanedPendingReviews(reviews, existingReviewIds = setOf(3L)).map { it.id })
  }

  @Test
  fun `reviews with comments or a body are kept`() {
    val reviews = listOf(pending(7, comments = 1), pending(8, body = "Looks good"))
    assertEquals(emptyList<Long>(), orphanedPendingReviews(reviews, existingReviewIds = emptySet()).map { it.id })
  }
}
