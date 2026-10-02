package com.github.jpmand.idea.plugin.gitea.ui.action

import org.junit.Assert.assertEquals
import org.junit.Test
import java.net.URI

class GiteaURLUtilTest {

    private val repo = URI("https://gitea.example.com/acme/webapp")
    private val sha = "e1aee3a56fabc20a19089594763eaec466815776"

    @Test
    fun `commit opens the commit page`() {
        assertEquals("https://gitea.example.com/acme/webapp/commit/$sha", GiteaURLUtil.getWebURI(repo, sha).toString())
    }

    @Test
    fun `file at a revision opens the file view`() {
        assertEquals(
            "https://gitea.example.com/acme/webapp/src/commit/$sha/src/App.java",
            GiteaURLUtil.getWebURI(repo, sha, "src/App.java", null).toString(),
        )
    }

    @Test
    fun `line range becomes the fragment`() {
        assertEquals(
            "https://gitea.example.com/acme/webapp/src/commit/$sha/README.md#L3-L5",
            GiteaURLUtil.getWebURI(repo, sha, "README.md", 2..4).toString(),
        )
        assertEquals(
            "https://gitea.example.com/acme/webapp/src/commit/$sha/README.md#L3",
            GiteaURLUtil.getWebURI(repo, sha, "README.md", 2..2).toString(),
        )
    }
}
