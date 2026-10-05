package com.github.jpmand.idea.plugin.gitea.pullrequest.diff

import com.github.jpmand.idea.plugin.gitea.api.models.GiteaCommit
import com.github.jpmand.idea.plugin.gitea.api.rest.pr.GiteaPRFileStatusEnum
import com.intellij.diff.util.Side
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GiteaPRDiffTargetTest {

  private val mergeBase = "base"

  @Test
  fun `the PR diff shows every thread and takes new comments`() {
    assertTrue(GiteaPRDiffTarget.PullRequest.showsThread("anything", Side.RIGHT, mergeBase))
    assertTrue(GiteaPRDiffTarget.PullRequest.showsThread(null, Side.LEFT, mergeBase))
    assertTrue(GiteaPRDiffTarget.PullRequest.allowsNewComments)
  }

  @Test
  fun `a commit's diff shows only the threads of reviews made on that commit`() {
    val commit = GiteaPRDiffTarget.Commit("c2", parentSha = "c1")
    assertTrue(commit.showsThread("c2", Side.RIGHT, mergeBase))
    assertFalse(commit.showsThread("c3", Side.RIGHT, mergeBase))
    assertFalse(commit.showsThread(null, Side.RIGHT, mergeBase))
  }

  @Test
  fun `a commit's diff shows old-side threads only when its parent is the merge base`() {
    assertFalse(GiteaPRDiffTarget.Commit("c2", parentSha = "c1").showsThread("c2", Side.LEFT, mergeBase))
    assertTrue(GiteaPRDiffTarget.Commit("c1", parentSha = mergeBase).showsThread("c1", Side.LEFT, mergeBase))
  }

  @Test
  fun `a commit's diff takes no new comments`() {
    assertFalse(GiteaPRDiffTarget.Commit("c2", parentSha = "c1").allowsNewComments)
  }

  @Test
  fun `the PR as of a commit shows only the threads of reviews made on that commit`() {
    val at = GiteaPRDiffTarget.PullRequestAt("c3")
    assertTrue(at.showsThread("c3", Side.RIGHT, mergeBase))
    assertTrue(at.showsThread("c3", Side.LEFT, mergeBase))
    assertFalse(at.showsThread("c1", Side.RIGHT, mergeBase))
    assertFalse(at.allowsNewComments)
  }

  // ── reviewThreadDiffTarget ──────────────────────────────────────────────

  private val head = "c4"

  private fun commit(sha: String, parent: String?) =
    GiteaCommit(sha, author = null, authorName = null, messageTitle = sha, messageBody = null, htmlUrl = null, createdAt = null, firstParentSha = parent)

  private fun file(name: String) = GiteaPRChangedFile(name, null, GiteaPRFileStatusEnum.MODIFIED, 0, 0, 0)

  private fun target(reviewCommit: GiteaCommit?, files: List<String>, path: String = "A.java", side: Side = Side.RIGHT) =
    reviewThreadDiffTarget(reviewCommit, files.map(::file), path, side, head, mergeBase)

  @Test
  fun `a thread opens the diff of its review's commit when that commit changed the file`() {
    assertEquals(GiteaPRDiffTarget.Commit("c1", mergeBase), target(commit("c1", mergeBase), listOf("A.java")))
  }

  @Test
  fun `a thread falls back to the PR as of its review's commit when that commit didn't change the file`() {
    assertEquals(GiteaPRDiffTarget.PullRequestAt("c3"), target(commit("c3", "c2"), listOf("Test.java")))
  }

  @Test
  fun `the fallback is the PR diff when the review's commit is still the head`() {
    assertEquals(GiteaPRDiffTarget.PullRequest, target(commit(head, "c3"), listOf("Test.java")))
  }

  @Test
  fun `a review on the head opens the head commit's diff when that commit changed the file`() {
    assertEquals(GiteaPRDiffTarget.Commit(head, "c3"), target(commit(head, "c3"), listOf("A.java")))
  }

  @Test
  fun `an old-side thread takes the commit's diff only when its parent is the merge base`() {
    assertEquals(GiteaPRDiffTarget.PullRequestAt("c2"), target(commit("c2", "c1"), listOf("A.java"), side = Side.LEFT))
    assertEquals(GiteaPRDiffTarget.Commit("c1", mergeBase), target(commit("c1", mergeBase), listOf("A.java"), side = Side.LEFT))
  }

  @Test
  fun `a thread without a review commit in the PR opens the PR diff`() {
    assertEquals(GiteaPRDiffTarget.PullRequest, target(null, emptyList()))
  }
}
