package com.github.jpmand.idea.plugin.gitea.pullrequest.ui.create

import org.junit.Assert.assertEquals
import org.junit.Test

class GiteaPRCreateDefaultsTest {

    @Test
    fun `one commit gives its subject and body`() {
        val commit = GiteaCommitMessage("Add cache\n\nKeeps the last 50 results.\nSecond line.")
        assertEquals("Add cache" to "Keeps the last 50 results.\nSecond line.", defaultTitleAndDescription("feature/cache", listOf(commit), null))
    }

    @Test
    fun `several commits give the branch name and no description`() {
        val commits = listOf(GiteaCommitMessage("One"), GiteaCommitMessage("Two"))
        assertEquals("Add cache layer" to "", defaultTitleAndDescription("feature/add-cache_layer", commits, null))
    }

    @Test
    fun `the template comes before the commit body`() {
        val commit = GiteaCommitMessage("Fix it\n\nDetails.")
        assertEquals("Fix it" to "## Summary\n\nDetails.", defaultTitleAndDescription("fix", listOf(commit), "## Summary\n"))
    }

    @Test
    fun `a blank template is ignored`() {
        assertEquals("Fix" to "", defaultTitleAndDescription("fix", emptyList(), "  \n"))
    }

    @Test
    fun `wip prefix is added once`() {
        assertEquals("WIP: Add cache", withWipPrefix("Add cache"))
        assertEquals("WIP: Add cache", withWipPrefix(" WIP: Add cache "))
        assertEquals("[WIP] Add cache", withWipPrefix("[WIP] Add cache"))
        assertEquals("wip: lower", withWipPrefix("wip: lower"))
    }

    @Test
    fun `wip prefix is removed in any of its forms`() {
        assertEquals("Add cache", withoutWipPrefix("WIP: Add cache"))
        assertEquals("Add cache", withoutWipPrefix(" [wip]  Add cache"))
        assertEquals("Add cache", withoutWipPrefix("Add cache"))
        assertEquals(false, hasWipPrefix("Add WIP: cache"))
    }

    @Test
    fun `templates are looked up in Gitea's order`() {
        assertEquals("PULL_REQUEST_TEMPLATE.md", PR_TEMPLATE_CANDIDATES.first())
        assertEquals(18, PR_TEMPLATE_CANDIDATES.size)
        assert(PR_TEMPLATE_CANDIDATES.indexOf(".gitea/pull_request_template.md") < PR_TEMPLATE_CANDIDATES.indexOf(".github/PULL_REQUEST_TEMPLATE.md"))
    }
}
