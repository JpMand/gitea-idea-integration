package com.github.jpmand.idea.plugin.gitea.api.rest

import com.github.jpmand.idea.plugin.gitea.api.rest.pr.pullRequestListUri
import org.junit.Assert.assertEquals
import org.junit.Test
import java.net.URI

class GiteaPullRequestListQueryTest {

  private val base = URI("http://localhost:3000/api/v1/repos/acme/webapp/pulls")

  @Test
  fun `labels are sent as one repeated param per label id`() {
    val uri = pullRequestListUri(base, state = "all", labels = listOf(1L, 3L), poster = "bob", limit = 50)
    assertEquals("state=all&labels=1&labels=3&poster=bob&limit=50", uri.rawQuery)
  }

  @Test
  fun `no labels param without a label filter`() {
    assertEquals("state=open", pullRequestListUri(base, state = "open").rawQuery)
  }
}
