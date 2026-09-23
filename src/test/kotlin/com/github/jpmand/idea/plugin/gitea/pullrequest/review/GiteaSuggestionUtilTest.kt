package com.github.jpmand.idea.plugin.gitea.pullrequest.review

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GiteaSuggestionUtilTest {

    @Test
    fun `single-line suggestion round-trips`() {
        val encoded = GiteaSuggestionUtil.encode(11, listOf("old line"), listOf("new line"))
        val body = "You should use xyz here.\n\n$encoded"

        val suggestion = GiteaSuggestionUtil.detect(body)

        assertEquals(GiteaSuggestion(11, listOf("old line"), listOf("new line")), suggestion)
    }

    @Test
    fun `multi-line suggestion round-trips, including a line-count expansion`() {
        val encoded = GiteaSuggestionUtil.encode(
            oldStartLine = 11,
            oldLines = listOf("old line 1", "old line 2"),
            newLines = listOf("new line 1", "new line 2", "new line 3"),
        )
        val body = "Split this into three lines.\n\n$encoded"

        val suggestion = GiteaSuggestionUtil.detect(body)

        assertEquals(
            GiteaSuggestion(11, listOf("old line 1", "old line 2"), listOf("new line 1", "new line 2", "new line 3")),
            suggestion,
        )
    }

    @Test
    fun `pure deletion (no replacement lines) round-trips`() {
        val encoded = GiteaSuggestionUtil.encode(11, listOf("delete me"), emptyList())

        val suggestion = GiteaSuggestionUtil.detect(encoded)

        assertEquals(GiteaSuggestion(11, listOf("delete me"), emptyList()), suggestion)
    }

    @Test
    fun `pure insertion (no original lines) round-trips`() {
        val encoded = GiteaSuggestionUtil.encode(11, emptyList(), listOf("inserted line"))

        val suggestion = GiteaSuggestionUtil.detect(encoded)

        assertEquals(GiteaSuggestion(11, emptyList(), listOf("inserted line")), suggestion)
    }

    @Test
    fun `no marker means no suggestion`() {
        assertNull(GiteaSuggestionUtil.detect("just a plain comment, no suggestion here"))
    }

    @Test
    fun `marker with no following fence means no suggestion`() {
        assertNull(GiteaSuggestionUtil.detect("<!--gitea-suggestion-->\nno fence follows this"))
    }

    @Test
    fun `marker with an unclosed fence means no suggestion`() {
        assertNull(GiteaSuggestionUtil.detect("<!--gitea-suggestion-->\n```diff\n@@ -1,1 +1,1 @@\n-a\n+b\n"))
    }

    @Test
    fun `marker with an empty fence means no suggestion`() {
        assertNull(GiteaSuggestionUtil.detect("<!--gitea-suggestion-->\n```diff\n```"))
    }

    @Test
    fun `only the first suggestion block is detected when a comment has extra diff fences`() {
        val encoded = GiteaSuggestionUtil.encode(11, listOf("old"), listOf("new"))
        val body = "$encoded\n\nunrelated:\n```diff\n@@ -50,1 +50,1 @@\n-x\n+y\n```"

        val suggestion = GiteaSuggestionUtil.detect(body)

        assertEquals(GiteaSuggestion(11, listOf("old"), listOf("new")), suggestion)
    }
}
