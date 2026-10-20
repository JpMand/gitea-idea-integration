package com.github.jpmand.idea.plugin.gitea.pullrequest.editor

import com.intellij.diff.util.LineRange
import com.intellij.diff.util.Range
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** A PR that turned base line 5 into head lines 5..6: `Range(5, 6, 5, 7)`. */
class GiteaPRChangesGutterTest {

  private val prRange = Range(5, 6, 5, 7)

  @Test
  fun `without local edits the PR ranges are the live ranges`() {
    assertEquals(listOf(prRange), shiftPRRanges(listOf(prRange), emptyList()))
  }

  @Test
  fun `lines added locally above a PR range move it down`() {
    assertEquals(listOf(Range(5, 6, 7, 9)), shiftPRRanges(listOf(prRange), listOf(Range(0, 0, 0, 2))))
  }

  @Test
  fun `lines removed locally above a PR range move it up`() {
    assertEquals(listOf(Range(5, 6, 4, 6)), shiftPRRanges(listOf(prRange), listOf(Range(1, 2, 1, 1))))
  }

  @Test
  fun `a local edit below a PR range leaves it in place`() {
    assertEquals(listOf(prRange), shiftPRRanges(listOf(prRange), listOf(Range(10, 11, 10, 12))))
  }

  @Test
  fun `the lines a local edit covers are cut out of a PR range`() {
    assertEquals(listOf(Range(5, 6, 5, 6)), shiftPRRanges(listOf(prRange), listOf(Range(6, 7, 6, 7))))
  }

  @Test
  fun `a PR range a local edit covers whole is left to the local changes bar`() {
    assertEquals(emptyList<Range>(), shiftPRRanges(listOf(prRange), listOf(Range(5, 7, 5, 8))))
  }

  @Test
  fun `base lines are cut without the last line's newline`() {
    val content = "a\nb\nc\n"
    assertEquals("b\nc", linesText(content, LineRange(1, 3)))
    assertEquals("a", linesText(content, LineRange(0, 1)))
    assertEquals("", linesText(content, LineRange(2, 2)))
    // The empty "line" after the final newline is a line, as in a document.
    assertEquals("", linesText(content, LineRange(3, 4)))
    assertNull(linesText(content, LineRange(3, 5)))
  }

  @Test
  fun `without local edits a live line is the same head line`() {
    assertEquals(7, liveToHeadLine(emptyList(), 7))
  }

  @Test
  fun `lines added locally above move the head line back up`() {
    // Two lines typed at live lines 3..4.
    assertEquals(5, liveToHeadLine(listOf(Range(3, 3, 3, 5)), 7))
  }

  @Test
  fun `a line typed locally shows the nearest head line`() {
    assertEquals(3, liveToHeadLine(listOf(Range(3, 3, 3, 5)), 4))
  }
}
