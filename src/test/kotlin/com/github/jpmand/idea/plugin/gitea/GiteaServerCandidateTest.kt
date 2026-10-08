package com.github.jpmand.idea.plugin.gitea

import com.intellij.testFramework.ApplicationRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.ClassRule
import org.junit.Test

/**
 * [giteaServerCandidate] turns a git remote URL into the server to probe for Gitea. Uses
 * [ApplicationRule] because it delegates URL parsing to git4idea's `GitHostingUrlUtil`.
 */
class GiteaServerCandidateTest {

  companion object {
    @ClassRule
    @JvmField
    val appRule = ApplicationRule()
  }

  private fun candidate(url: String) = giteaServerCandidate(url)?.toString()

  @Test
  fun `https remote`() {
    assertEquals("https://gitea.example.com", candidate("https://gitea.example.com/acme/widgets.git"))
    assertEquals("https://gitea.example.com", candidate("https://gitea.example.com/acme/widgets"))
    assertEquals("https://gitea.example.com", candidate("https://gitea.example.com/acme/widgets/"))
  }

  @Test
  fun `http remote keeps its port`() {
    assertEquals("http://localhost:3000", candidate("http://localhost:3000/acme/webapp.git"))
    assertEquals("https://gitea.example.com:8443", candidate("https://gitea.example.com:8443/acme/widgets.git"))
  }

  @Test
  fun `server on a sub-path`() {
    assertEquals("https://example.com/gitea", candidate("https://example.com/gitea/acme/widgets.git"))
    assertEquals("https://example.com/tools/gitea", candidate("https://example.com/tools/gitea/acme/widgets"))
  }

  @Test
  fun `credentials in the url are not part of the server`() {
    assertEquals("http://localhost:3000", candidate("http://alice:secret@localhost:3000/acme/webapp.git"))
  }

  @Test
  fun `ssh remotes are assumed to be served over https`() {
    assertEquals("https://gitea.example.com", candidate("git@gitea.example.com:acme/widgets.git"))
    assertEquals("https://gitea.example.com", candidate("ssh://git@gitea.example.com:2222/acme/widgets.git"))
  }

  @Test
  fun `a path too short for owner and repo has no candidate`() {
    assertNull(candidate("https://gitea.example.com/widgets.git"))
    assertNull(candidate("https://gitea.example.com"))
  }

  @Test
  fun `well-known non-Gitea hosts are not probed`() {
    assertNull(candidate("https://github.com/acme/widgets.git"))
    assertNull(candidate("git@gitlab.com:acme/widgets.git"))
    assertNull(candidate("https://bitbucket.org/acme/widgets.git"))
  }
}
