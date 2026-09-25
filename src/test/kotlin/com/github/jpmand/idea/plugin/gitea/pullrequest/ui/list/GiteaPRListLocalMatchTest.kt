package com.github.jpmand.idea.plugin.gitea.pullrequest.ui.list

import com.github.jpmand.idea.plugin.gitea.api.GiteaJsonDeSerializer
import com.github.jpmand.idea.plugin.gitea.api.models.GiteaPullRequest
import com.github.jpmand.idea.plugin.gitea.api.rest.dto.PullRequest
import com.github.jpmand.idea.plugin.gitea.pullrequest.ui.filters.GiteaPRListSearchValue
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File
import java.io.FileReader

/** The search text is matched locally, page by page, as the PR list loads. */
class GiteaPRListLocalMatchTest {

  private val prs: List<GiteaPullRequest> =
    FileReader(File("src/test/testData/pull_request_list.json")).use { reader ->
      GiteaJsonDeSerializer.fromJson(reader, Array<PullRequest>::class.java)!!.map { GiteaPullRequest.fromDto(it) }
    }

  private fun matching(search: GiteaPRListSearchValue) = prs.filter { search.matchesLocally(it) }.map { it.number }

  @Test
  fun `no search text matches everything`() {
    assertEquals(prs.map { it.number }, matching(GiteaPRListSearchValue.DEFAULT))
  }

  @Test
  fun `search text matches the title case-insensitively`() {
    assertEquals(listOf(17L), matching(GiteaPRListSearchValue(searchQuery = "session expiration")))
  }

  @Test
  fun `search text matches the PR number`() {
    assertEquals(listOf(18L), matching(GiteaPRListSearchValue(searchQuery = "#18")))
  }

  @Test
  fun `a label no PR carries matches nothing`() {
    assertEquals(emptyList<Long>(), matching(GiteaPRListSearchValue(label = "no-such-label")))
  }
}
