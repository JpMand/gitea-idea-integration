package com.github.jpmand.idea.plugin.gitea.api.json

import com.github.jpmand.idea.plugin.gitea.api.GiteaJsonDeSerializer
import com.github.jpmand.idea.plugin.gitea.api.models.GiteaLabel
import com.github.jpmand.idea.plugin.gitea.api.rest.dto.Label
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File
import java.io.FileReader

/**
 * Validates that the [Label] DTO parses a real Gitea `/repos/{owner}/{repo}/labels` response
 * (fixture at src/test/testData/labels.json) — the source for the PR-list "Label" filter.
 */
class GiteaJsonLabelTest {

  private fun fixture(): File = File("src/test/testData/labels.json")

  @Test
  fun `deserializes label list fixture`() {
    val labels = FileReader(fixture()).use { reader ->
      GiteaJsonDeSerializer.fromJson(reader, Array<Label>::class.java)
    }!!.map { GiteaLabel.fromDto(it) }

    assertEquals(3, labels.size)
    assertEquals("bug", labels[0].name)
    assertEquals("d73a4a", labels[0].color)
    assertEquals(1L, labels[0].id)
    assertEquals("good first issue", labels[2].name)
  }
}
