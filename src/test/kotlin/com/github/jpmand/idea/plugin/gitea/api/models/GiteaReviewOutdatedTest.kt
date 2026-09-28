package com.github.jpmand.idea.plugin.gitea.api.models

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Hunks below are real `diff_hunk` values returned by Gitea 1.27.3 for comments on
 * `src/main/java/com/acme/Util.java` (lines 7 and 3), before and after a push that rewrote line 7.
 */
class GiteaReviewOutdatedTest {

  private val hunkLine7 =
    "@@ -5,3 +5,3 @@\n \n     public static String trim(String s) {\n-        return s.trim();\n+        return s == null ? \"\" : s.trim();"
  private val hunkLine3 = "@@ -1,3 +1,3 @@\n package com.acme;\n \n public final class Util {\n"

  private val headAfterPush = """
    package com.acme;

    public final class Util {
        private Util() {}

        public static String trim(String s) {
            return s == null ? null : s.strip();
        }
    }
  """.trimIndent().lines()

  private fun comment(newLine: Int?, hunk: String?) = GiteaReviewComment(
    id = 1, author = null, body = "c", createdAt = null, updatedAt = null,
    path = "Util.java", newLine = newLine, oldLine = null, diffHunk = hunk,
    commitId = "86af841", originalCommitId = null, reviewId = 1L, resolver = null,
  )

  @Test
  fun `anchored line text is the last hunk line without its prefix`() {
    assertEquals("        return s == null ? \"\" : s.trim();", comment(7, hunkLine7).anchoredLineText())
    assertEquals("public final class Util {", comment(3, hunkLine3).anchoredLineText())
  }

  @Test
  fun `rewritten line is outdated`() {
    assertTrue(isAnchorOutdated(comment(7, hunkLine7), headAfterPush))
  }

  @Test
  fun `untouched line stays current even though its commit id is not the head`() {
    assertFalse(isAnchorOutdated(comment(3, hunkLine3), headAfterPush))
  }

  @Test
  fun `line past the end of the file is outdated`() {
    assertTrue(isAnchorOutdated(comment(3, hunkLine3), listOf("")))
  }

  @Test
  fun `comments without a hunk or on a removed line are never outdated`() {
    assertFalse(isAnchorOutdated(comment(3, null), headAfterPush))
    val removedLine = comment(null, "@@ -5,1 +5,0 @@\n-        return s.trim();")
    assertNull(removedLine.anchoredLineText())
    assertFalse(isAnchorOutdated(removedLine, headAfterPush))
  }
}
