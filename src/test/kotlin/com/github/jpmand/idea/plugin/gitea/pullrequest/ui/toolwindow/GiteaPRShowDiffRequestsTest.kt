package com.github.jpmand.idea.plugin.gitea.pullrequest.ui.toolwindow

import com.github.jpmand.idea.plugin.gitea.api.models.GiteaReviewThread
import com.github.jpmand.idea.plugin.gitea.pullrequest.diff.GiteaPRDiffTarget
import com.intellij.collaboration.ui.codereview.diff.model.DiffViewerScrollRequest
import com.intellij.diff.util.Side
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

@Suppress("UnstableApiUsage")
class GiteaPRShowDiffRequestsTest {

    private fun thread(newLine: Int?, oldLine: Int?, outdated: Boolean = false) =
        GiteaReviewThread(1, "src/App.java", newLine, oldLine, isResolved = false, comments = emptyList(), isOutdated = outdated)

    @Test
    fun `new-side comment scrolls to its 0-based line on the right`() {
        assertEquals(DiffViewerScrollRequest.toLine(Side.RIGHT to 9), thread(newLine = 10, oldLine = null).diffScrollRequest())
    }

    @Test
    fun `old-side comment scrolls to its 0-based line on the left`() {
        assertEquals(DiffViewerScrollRequest.toLine(Side.LEFT to 3), thread(newLine = null, oldLine = 4).diffScrollRequest())
    }

    @Test
    fun `outdated thread opens the PR diff without scrolling`() {
        assertNull(thread(newLine = 10, oldLine = null, outdated = true).diffScrollRequest())
    }

    @Test
    fun `file-level comment opens the file without scrolling`() {
        assertNull(thread(newLine = null, oldLine = null).diffScrollRequest())
    }

    @Test
    fun `outdated thread scrolls to its line in the diff of its review's commit`() {
        val expected = DiffViewerScrollRequest.toLine(Side.RIGHT to 9)
        val thread = thread(newLine = 10, oldLine = null, outdated = true)
        assertEquals(expected, thread.diffScrollRequest(GiteaPRDiffTarget.Commit("c1", "base")))
        assertEquals(expected, thread.diffScrollRequest(GiteaPRDiffTarget.PullRequestAt("c3")))
    }
}
