package com.github.jpmand.idea.plugin.gitea.api.json

import com.github.jpmand.idea.plugin.gitea.api.GiteaJsonDeSerializer
import com.github.jpmand.idea.plugin.gitea.api.models.GiteaReviewComment
import com.github.jpmand.idea.plugin.gitea.api.rest.dto.PullReviewComment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.File
import java.io.FileReader

/** A review comment on a new-side line, as returned by Gitea 1.27.3 (`original_position` is 0). */
class GiteaJsonReviewCommentTest {

  @Test
  fun `the side without a line has no line number`() {
    val dto = FileReader(File("src/test/testData/pull_review_comment.json")).use { reader ->
      GiteaJsonDeSerializer.fromJson(reader, PullReviewComment::class.java)
    }!!
    assertEquals(0, dto.originalPosition)

    val comment = GiteaReviewComment.fromDto(dto)
    assertEquals(7, comment.newLine)
    assertNull(comment.oldLine)
  }
}
