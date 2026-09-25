package com.github.jpmand.idea.plugin.gitea.pullrequest.editor

import com.github.jpmand.idea.plugin.gitea.pullrequest.review.GiteaSuggestion
import com.github.jpmand.idea.plugin.gitea.pullrequest.ui.editor.contentLineCount
import com.intellij.diff.util.Range
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * A 3-line head file ending in a newline. The platform's diff sees it as 4 lines (the last one
 * empty), so its ranges can reach line index 3, which Gitea doesn't have.
 */
class GiteaSuggestionForRangeTest {

  private val headContent = "a\nb\nc\n"
  private val headLines = headContent.lines().take(contentLineCount(headContent))

  @Test
  fun `a final newline doesn't count as a line`() {
    assertEquals(3, contentLineCount("a\nb\nc\n"))
    assertEquals(3, contentLineCount("a\nb\nc"))
    assertEquals(1, contentLineCount("\n"))
    assertEquals(0, contentLineCount(""))
  }

  @Test
  fun `appending after the last line anchors on the last line`() {
    // Typed over the empty "line" after the final newline: the diff reports it replaced.
    val (anchor, suggestion) = suggestionForRange(Range(3, 4, 3, 5), headLines, listOf("d", "e"))!!
    assertEquals(2, anchor)
    assertEquals(GiteaSuggestion(3, emptyList(), listOf("d", "e")), suggestion)
  }

  @Test
  fun `editing the last line and appending keeps the last line as the replaced one`() {
    val (anchor, suggestion) = suggestionForRange(Range(2, 4, 2, 5), headLines, listOf("C", "d", "e"))!!
    assertEquals(2, anchor)
    assertEquals(GiteaSuggestion(2, listOf("c"), listOf("C", "d", "e")), suggestion)
  }

  @Test
  fun `a replacement inside the file is unchanged`() {
    val (anchor, suggestion) = suggestionForRange(Range(1, 2, 1, 2), headLines, listOf("B"))!!
    assertEquals(1, anchor)
    assertEquals(GiteaSuggestion(1, listOf("b"), listOf("B")), suggestion)
  }

  @Test
  fun `an insertion at the top anchors on the first line`() {
    val (anchor, suggestion) = suggestionForRange(Range(0, 0, 0, 1), headLines, listOf("z"))!!
    assertEquals(0, anchor)
    assertEquals(GiteaSuggestion(0, emptyList(), listOf("z")), suggestion)
  }

  @Test
  fun `an empty head file has nothing to anchor on`() {
    assertNull(suggestionForRange(Range(0, 1, 0, 1), emptyList(), listOf("x")))
  }
}
