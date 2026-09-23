package com.github.jpmand.idea.plugin.gitea.api

import com.intellij.testFramework.ApplicationRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.ClassRule
import org.junit.Test

/**
 * [GiteaRepositoryPath.create] parses a git remote URL into `owner/repo` relative to a configured
 * server. Uses [ApplicationRule] because it delegates URL parsing to git4idea's `GitHostingUrlUtil`.
 */
class GiteaRepositoryPathTest {

  companion object {
    @ClassRule
    @JvmField
    val appRule = ApplicationRule()
  }

  private val server = GiteaServerPath.from("https://gitea.example.com")
  private val subPathServer = GiteaServerPath.from("https://example.com/gitea")

  private fun path(server: GiteaServerPath, url: String) = GiteaRepositoryPath.create(server, url)

  @Test
  fun `https remote with dot-git suffix`() {
    val p = path(server, "https://gitea.example.com/acme/widgets.git")
    assertEquals("acme", p?.owner)
    assertEquals("widgets", p?.repository)
  }

  @Test
  fun `https remote without dot-git suffix`() {
    val p = path(server, "https://gitea.example.com/acme/widgets")
    assertEquals("acme", p?.owner)
    assertEquals("widgets", p?.repository)
  }

  @Test
  fun `ssh scp-like remote`() {
    val p = path(server, "git@gitea.example.com:acme/widgets.git")
    assertEquals("acme", p?.owner)
    assertEquals("widgets", p?.repository)
  }

  @Test
  fun `ssh url remote with custom port`() {
    val p = path(server, "ssh://git@gitea.example.com:2222/acme/widgets.git")
    assertEquals("acme", p?.owner)
    assertEquals("widgets", p?.repository)
  }

  @Test
  fun `server hosted on a context sub-path`() {
    val p = path(subPathServer, "https://example.com/gitea/acme/widgets.git")
    assertEquals("acme", p?.owner)
    assertEquals("widgets", p?.repository)
  }

  @Test
  fun `remote whose path does not sit under the server is not matched`() {
    assertNull(path(subPathServer, "https://example.com/other/acme/widgets.git"))
  }

  @Test
  fun `fullPath round-trips`() {
    val p = path(server, "https://gitea.example.com/acme/widgets.git")
    assertEquals("acme/widgets", p?.fullPath())
    assertEquals("widgets", p?.fullPath(withOwner = false))
  }
}
