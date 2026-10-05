package com.github.jpmand.idea.plugin.gitea.pullrequest.diff

import com.intellij.diff.util.Side
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
}
