package com.github.jpmand.idea.plugin.gitea.api.models

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GiteaReviewThreadTest {

  private fun comment(id: Long, path: String?, newLine: Int?, oldLine: Int? = null, resolved: Boolean = false, reviewId: Long = 1L) =
    GiteaReviewComment(
      id = id, author = null, body = "c$id", createdAt = null, updatedAt = null,
      path = path, newLine = newLine, oldLine = oldLine, diffHunk = null,
      commitId = null, originalCommitId = null, reviewId = reviewId,
      resolver = if (resolved) GiteaUser(id = 9, login = "r", email = null, fullName = null, avatarUrl = null, htmlUrl = null) else null,
    )

  @Test
  fun `comments at the same location form one thread, ordered by id`() {
    val threads = listOf(comment(3, "a.kt", 10), comment(1, "a.kt", 10), comment(2, "a.kt", 10)).toThreads()
    assertEquals(1, threads.size)
    assertEquals(listOf(1L, 2L, 3L), threads.single().comments.map { it.id })
    assertEquals(1L, threads.single().id)
  }

  @Test
  fun `different locations produce different threads`() {
    val threads = listOf(comment(1, "a.kt", 10), comment(2, "a.kt", 20), comment(3, "b.kt", 10)).toThreads()
    assertEquals(3, threads.size)
  }

  @Test
  fun `path-less comments are kept as their own thread`() {
    val threads = listOf(comment(1, "a.kt", 10), comment(2, null, null)).toThreads()
    assertEquals(2, threads.size)
    assertTrue(threads.any { it.path == null && it.newLine == null && it.oldLine == null })
  }

  @Test
  fun `resolution state comes from the anchor comment`() {
    val threads = listOf(comment(1, "a.kt", 10, resolved = true), comment(2, "a.kt", 10)).toThreads()
    assertTrue(threads.single().isResolved)
  }

  private fun review(id: Long, commitId: String?) = GiteaReview(
    id = id, author = null, body = null, state = GiteaReviewState.COMMENT, submittedAt = null,
    dismissed = false, stale = false, commitId = commitId, commentsCount = 1, htmlUrl = "",
  )

  @Test
  fun `a thread takes the commit of its anchor comment's review`() {
    val threads = listOf(comment(1, "a.kt", 10, reviewId = 5), comment(2, "a.kt", 20, reviewId = 7))
      .toThreads()
      .withReviewCommits(listOf(review(5, "c3"), review(7, "c1")))
    assertEquals(listOf("c3", "c1"), threads.map { it.reviewCommitId })
  }

  @Test
  fun `a thread knows the commits of all its comments' reviews`() {
    val thread = listOf(comment(1, "a.kt", 19, reviewId = 5), comment(2, "a.kt", 19, reviewId = 7))
      .toThreads()
      .withReviewCommits(listOf(review(5, "c3"), review(7, "c1")))
      .single()
    assertEquals("c3", thread.reviewCommitId)
    assertEquals(setOf("c3", "c1"), thread.reviewCommitIds)
  }

  @Test
  fun `a review without a commit leaves the thread's review commit unset`() {
    val threads = listOf(comment(1, "a.kt", 10, reviewId = 5), comment(2, "a.kt", 20, reviewId = 6))
      .toThreads()
      .withReviewCommits(listOf(review(5, ""), review(6, null)))
    assertEquals(listOf(null, null), threads.map { it.reviewCommitId })
    assertEquals(listOf(emptySet<String>(), emptySet<String>()), threads.map { it.reviewCommitIds })
  }
}
