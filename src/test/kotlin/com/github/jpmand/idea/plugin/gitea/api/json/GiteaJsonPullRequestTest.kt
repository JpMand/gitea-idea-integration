package com.github.jpmand.idea.plugin.gitea.api.json

import com.github.jpmand.idea.plugin.gitea.api.GiteaJsonDeSerializer
import com.github.jpmand.idea.plugin.gitea.api.rest.dto.PullRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.io.FileReader

/**
 * Validates that [PullRequest] (the Gitea-Swagger-generated DTO introduced to replace the old
 * hand-rolled `GiteaPullRequestDTO`) correctly parses a real Gitea `/repos/{owner}/{repo}/pulls`
 * API response, using the fixture recorded at src/test/testData/pull_request_list.json.
 */
class GiteaJsonPullRequestTest {

  private fun fixture(): File = File("src/test/testData/pull_request_list.json")

  @Test
  fun `deserializes pull request list fixture`() {
    val prs = FileReader(fixture()).use { reader ->
      GiteaJsonDeSerializer.fromJson(reader, Array<PullRequest>::class.java)
    }

    assertNotNull(prs)
    assertTrue(prs!!.isNotEmpty())
    assertEquals(10, prs.size)

    val first = prs[0]
    assertEquals(135516L, first.id)
    assertEquals(19L, first.number)
    assertEquals("WIP: asdf", first.title)
    assertEquals(PullRequest.State.OPEN, first.state)
    assertTrue(first.draft == true)
    assertNotNull(first.user)
    assertEquals("pasture3992", first.user?.login)
    assertNotNull(first.base)
    assertEquals("95499115ec0208ff6728c92cd5ec4fe939197b69", first.base?.sha)
  }
}
