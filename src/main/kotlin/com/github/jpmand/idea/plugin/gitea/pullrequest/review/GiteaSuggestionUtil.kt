package com.github.jpmand.idea.plugin.gitea.pullrequest.review

import com.intellij.openapi.diff.impl.patch.PatchHunkUtil
import com.intellij.openapi.diff.impl.patch.PatchLine
import com.intellij.openapi.diff.impl.patch.PatchReader

/**
 * A reviewer's proposed replacement for a contiguous range of lines — this plugin's emulation of
 * GitHub's "suggested change," which Gitea has no native equivalent of. Encoded as a marked
 * unified-diff hunk inside an otherwise ordinary review comment body (see [GiteaSuggestionUtil]),
 * so it still renders as a normal syntax-highlighted diff in Gitea's own web UI.
 *
 * [oldStartLine] is 0-indexed, in the file's current (head-SHA) line numbering — the same
 * convention [com.intellij.openapi.diff.impl.patch.PatchHunk.getStartLineBefore] and
 * [com.github.jpmand.idea.plugin.gitea.pullrequest.ui.editor.GiteaPRLiveDiffSync] already use, so
 * callers translating between the two don't need to re-index.
 */
data class GiteaSuggestion(
    val oldStartLine: Int,
    val oldLines: List<String>,
    val newLines: List<String>,
)

/**
 * Encodes/detects a [GiteaSuggestion] as a marker + unified-diff-hunk fragment inside a review
 * comment body. At most one suggestion per comment (matches GitHub's own constraint) — [detect]
 * looks for the marker followed by the *next* fenced ` ```diff ` block, not a matched pair of
 * markers.
 *
 * The marker is an HTML comment, not a custom tag: a same-line HTML comment is a self-contained
 * CommonMark raw-HTML block (it never swallows following content, regardless of blank lines),
 * whereas an unknown custom tag starts a block that extends until the next blank line — which
 * would swallow the ` ```diff ` fence into raw HTML and prevent it from being parsed as a code
 * fence (no syntax highlighting) unless blank lines are placed exactly around it. Whether the tag
 * itself stays invisible after Gitea's sanitizer runs is a second risk a comment avoids entirely
 * (sanitizers drop or hide comments; some escape unknown tags to visible text instead of
 * stripping them).
 *
 * The hunk itself is a genuine unified-diff hunk — same format Gitea's own `diffHunk` field
 * already uses on ordinary review comments, so [detect] reuses the exact
 * [PatchHunkUtil]/[PatchReader] machinery
 * [com.github.jpmand.idea.plugin.gitea.pullrequest.ui.timeline.GiteaPRTimelineItemComponentFactory.diffHunkComponent]
 * already relies on to parse it, rather than hand-rolling a parser.
 */
object GiteaSuggestionUtil {
    private const val MARKER = "<!--gitea-suggestion-->"
    private const val FENCE = "```diff"

    /**
     * Builds the marker + diff-hunk fragment for a suggestion replacing [oldStartLine] (0-indexed,
     * in the current/head-SHA file) through `oldStartLine + oldLines.size` with [newLines].
     * Callers append this after their own explanation text in the comment body.
     */
    fun encode(oldStartLine: Int, oldLines: List<String>, newLines: List<String>): String {
        val header = "@@ -${oldStartLine + 1},${oldLines.size} +${oldStartLine + 1},${newLines.size} @@"
        val hunkBody = buildString {
            append(header)
            oldLines.forEach { append('\n').append('-').append(it) }
            newLines.forEach { append('\n').append('+').append(it) }
        }
        return "$MARKER\n$FENCE\n$hunkBody\n```"
    }

    /** Returns the suggestion encoded in [commentBody], or `null` if there isn't one (no marker,
     * no fenced diff block following it, or the hunk fails to parse). */
    fun detect(commentBody: String): GiteaSuggestion? {
        val markerIdx = commentBody.indexOf(MARKER)
        if (markerIdx < 0) return null
        val afterMarker = commentBody.substring(markerIdx + MARKER.length)

        val fenceStart = afterMarker.indexOf(FENCE)
        if (fenceStart < 0) return null
        val contentStart = afterMarker.indexOf('\n', fenceStart)
        if (contentStart < 0) return null
        val fenceEnd = afterMarker.indexOf("```", contentStart + 1)
        if (fenceEnd < 0) return null
        val hunkText = afterMarker.substring(contentStart + 1, fenceEnd).trim('\n')
        if (hunkText.isBlank()) return null

        // createPatchFromHunk needs *some* file path to synthesize --- a//+++ b headers with —
        // the suggestion's own target file (comment.path) is irrelevant to parsing the hunk's
        // line numbers/content, so a placeholder is fine here.
        val hunk = try {
            PatchReader(PatchHunkUtil.createPatchFromHunk("suggestion", hunkText))
                .readTextPatches().firstOrNull()?.hunks?.firstOrNull()
        } catch (e: Exception) {
            null
        } ?: return null

        val oldLines = hunk.lines.filter { it.type == PatchLine.Type.REMOVE }.map { it.text }
        val newLines = hunk.lines.filter { it.type == PatchLine.Type.ADD }.map { it.text }
        if (oldLines.isEmpty() && newLines.isEmpty()) return null
        return GiteaSuggestion(hunk.startLineBefore, oldLines, newLines)
    }

    /** The comment body with its suggestion block (if any) removed — everything before the
     * marker, trimmed. Callers render this instead of the raw body when [detect] finds a
     * suggestion, then render the suggestion itself as its own component alongside it. */
    fun stripSuggestion(commentBody: String): String {
        val markerIdx = commentBody.indexOf(MARKER)
        return if (markerIdx < 0) commentBody else commentBody.substring(0, markerIdx).trimEnd()
    }
}
