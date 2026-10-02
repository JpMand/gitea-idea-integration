package com.github.jpmand.idea.plugin.gitea.pullrequest.ui.timeline

import com.intellij.openapi.diff.impl.patch.PatchHunkUtil
import com.intellij.openapi.diff.impl.patch.PatchLine
import com.intellij.openapi.diff.impl.patch.PatchReader
import org.junit.Assert.assertEquals
import org.junit.Test

class GiteaDiffHunksTest {

    private fun parse(hunk: String) =
        PatchReader(PatchHunkUtil.createPatchFromHunk("myfile.txt", hunk)).readTextPatches().single().hunks.single()

    /** What Gitea stores for a comment on a line right after a "No newline at end of file" marker. */
    @Test
    fun `hunk Gitea cut after a no-newline marker restarts after it`() {
        val gitea = "@@ -2,1 +1,4 @@\n\\ No newline at end of file\n+This is a test.\n+\n+with changes"

        val normalized = normalizeDiffHunk(gitea)

        assertEquals("@@ -2,0 +2,3 @@\n+This is a test.\n+\n+with changes", normalized)
        val hunk = parse(normalized)
        assertEquals(listOf("This is a test.", "", "with changes"), hunk.lines.map { it.text })
        assertEquals(List(3) { PatchLine.Type.ADD }, hunk.lines.map { it.type })
        assertEquals(1, hunk.startLineAfter)
    }

    @Test
    fun `marker inside the window drops the lines before it`() {
        val hunk = " a\n-b\n\\ No newline at end of file\n+b\n+c"

        assertEquals("@@ -2,0 +2,2 @@\n+b\n+c", normalizeDiffHunk("@@ -1,2 +1,3 @@\n$hunk"))
    }

    @Test
    fun `marker after the last line is kept`() {
        val hunk = "@@ -1,2 +1,2 @@\n a\n-b\n+c\n\\ No newline at end of file"

        assertEquals(hunk, normalizeDiffHunk(hunk))
        assertEquals(3, parse(hunk).lines.size)
    }

    @Test
    fun `well formed hunk is unchanged`() {
        val hunk = "@@ -28,12 +28,6 @@ class Foo\n line1\n-line2\n-line3\n+line4\n line5"

        assertEquals(hunk, normalizeDiffHunk(hunk))
    }

    @Test
    fun `text without a hunk header is unchanged`() {
        assertEquals("not a hunk", normalizeDiffHunk("not a hunk"))
    }
}
