package com.github.jpmand.idea.plugin.gitea.pullrequest.review

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GiteaReviewSubmitProblemTest {

  @Test
  fun `request changes needs a body even with inline comments`() {
    assertEquals(
      "pull.request.review.submit.error.request.changes.body",
      reviewSubmitProblem(GiteaReviewVerdict.REQUEST_CHANGES, "  ", commentCount = 2),
    )
    assertNull(reviewSubmitProblem(GiteaReviewVerdict.REQUEST_CHANGES, "Please fix", commentCount = 0))
  }

  @Test
  fun `comment needs a body or an inline comment`() {
    assertEquals("pull.request.review.submit.error.empty", reviewSubmitProblem(GiteaReviewVerdict.COMMENT, "", 0))
    assertNull(reviewSubmitProblem(GiteaReviewVerdict.COMMENT, "", commentCount = 1))
    assertNull(reviewSubmitProblem(GiteaReviewVerdict.COMMENT, "LGTM", commentCount = 0))
  }

  @Test
  fun `approve and pending never need a body`() {
    assertNull(reviewSubmitProblem(GiteaReviewVerdict.APPROVE, "", 0))
    assertNull(reviewSubmitProblem(GiteaReviewVerdict.PENDING, "", 0))
  }
}
