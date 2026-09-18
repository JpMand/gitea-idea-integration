package com.github.jpmand.idea.plugin.gitea.pullrequest.editor

import com.github.jpmand.idea.plugin.gitea.pullrequest.review.GiteaSuggestion
import com.github.jpmand.idea.plugin.gitea.pullrequest.ui.editor.GiteaPRDiffEditorModel
import com.github.jpmand.idea.plugin.gitea.pullrequest.ui.editor.GiteaPRLiveDiffSync
import com.github.jpmand.idea.plugin.gitea.util.GiteaBundle
import com.intellij.diff.util.Range
import com.intellij.diff.util.Side
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.markup.*
import com.intellij.openapi.editor.markup.LineMarkerRendererEx.Position
import com.intellij.openapi.util.TextRange
import com.intellij.ui.JBColor
import com.intellij.ui.scale.JBUIScale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.awt.Color
import java.awt.Graphics
import java.awt.Rectangle
import java.awt.event.MouseEvent

/**
 * Adds this plugin's own colored gutter bar — mirroring how IntelliJ's native "local changes"
 * bar already marks edited lines, but in its own color, immediately beside the native bar rather
 * than overlapping it (see [SuggestionGutterBarRenderer]'s doc for why that placement is manual
 * rather than [LineMarkerRendererEx.Position.RIGHT]) — on every locally-edited line range that has
 * replacement content to offer as a suggestion: every [sync] range with at least one live line
 * ([Range.start2] `<` [Range.end2]). A pure deletion (`start2 == end2`) has nothing to show as a
 * suggestion and is skipped, out of scope for v1.
 *
 * A plain [com.intellij.openapi.editor.markup.GutterIconRenderer] (a lightbulb, tried first) reads
 * as IntelliJ's own "show context actions" intention bulb — this bar reads as what it actually is,
 * an extra piece of VCS-style gutter chrome, and (via [ActiveGutterRenderer]) is clickable across
 * its whole height, not just one line.
 *
 * Clicking anywhere on the bar builds a [GiteaSuggestion] straight from [headContent] (the range's
 * *before* side — the PR's actual head SHA, not local git HEAD, which [sync] is already anchored
 * to) and the live document (the range's *after* side), then hands it to [model]'s composer via
 * [GiteaPRDiffEditorModel.requestSuggestion]. The comment anchor passed along is the range's own
 * before-side boundary ([Range.start1]/[Range.end1] — real head-SHA lines, always present) rather
 * than a [GiteaPRLiveDiffSync.liveToAnchor] lookup on a live line — every live line in the range is
 * itself part of the edit and so has no head-SHA counterpart to look up.
 */
fun CoroutineScope.installSuggestionGutterIcons(
    editor: EditorEx,
    sync: GiteaPRLiveDiffSync,
    headContent: String,
    model: GiteaPRDiffEditorModel,
) {
    val headLines = headContent.lines()
    var highlighters: List<RangeHighlighter> = emptyList()

    launch {
        sync.ranges.collect { ranges ->
            highlighters.forEach { it.dispose() }
            highlighters = ranges.filter { it.start2 < it.end2 }.map { range ->
                addSuggestionGutterBar(editor, headLines, range, model)
            }
        }
    }
}

private fun addSuggestionGutterBar(
    editor: EditorEx,
    headLines: List<String>,
    range: Range,
    model: GiteaPRDiffEditorModel,
): RangeHighlighter {
    val document = editor.document
    val startOffset = document.getLineStartOffset(range.start2)
    val endOffset = if (range.end2 < document.lineCount) document.getLineStartOffset(range.end2) else document.textLength
    val highlighter = editor.markupModel.addRangeHighlighter(
        startOffset, endOffset, HighlighterLayer.ERROR + 1, null, HighlighterTargetArea.LINES_IN_RANGE,
    )
    highlighter.lineMarkerRenderer = SuggestionGutterBarRenderer {
        // Coerced defensively: headLines comes from Kotlin's own String.lines() while
        // range.start1/end1 come from the platform's Document-based diff Range — a mismatch at a
        // trailing-newline boundary between the two line-splitting conventions would otherwise
        // throw IndexOutOfBoundsException synchronously on the EDT from this click handler.
        val start1 = range.start1.coerceIn(0, headLines.size)
        val end1 = range.end1.coerceIn(start1, headLines.size)
        val oldLines = headLines.subList(start1, end1)
        val newLines = (range.start2 until range.end2).map { line ->
            document.getText(TextRange(document.getLineStartOffset(line), document.getLineEndOffset(line)))
        }
        val suggestion = GiteaSuggestion(start1, oldLines, newLines)
        // The range's before-side boundary (a real head-SHA line — always present, unlike any
        // live line inside the edit itself) doubles as the comment's anchor: the last replaced
        // line when there is one, otherwise the line right before a pure insertion.
        val anchorLine = if (start1 < end1) end1 - 1 else (start1 - 1).coerceAtLeast(0)
        model.requestSuggestion(range.end2 - 1, Side.RIGHT, anchorLine, suggestion)
    }
    return highlighter
}

private val SUGGESTION_BAR_COLOR: Color = JBColor(0x0E8577, 0x3FB6A8)
private const val BAR_WIDTH = 4

/**
 * [LineMarkerRendererEx.Position.RIGHT] turned out to render nothing — that sub-column only gets a
 * non-zero width once something reserves it via [com.intellij.openapi.editor.ex.EditorGutterComponentEx]'s
 * `@ApiStatus.Internal`-gated `reserveRightFreePaintersAreaWidth`, off limits here — while mouse
 * hit-testing for an [ActiveGutterRenderer] isn't clipped to that width, which is exactly why the
 * bar was invisible yet still fully clickable/tooltip-able. [Position.CUSTOM] instead hands full
 * manual control of the paint rectangle: this renderer draws a fixed-width bar of its own, anchored
 * to [com.intellij.openapi.editor.ex.EditorGutterComponentEx.getIconAreaOffset] (both public,
 * stable getters) so it always lands inside the gutter's existing, already-nonzero line-marker
 * column — immediately beside IntelliJ's own native VCS change bar rather than overlapping it.
 */
private class SuggestionGutterBarRenderer(private val onClick: () -> Unit) : ActiveGutterRenderer, LineMarkerRendererEx {
    override fun getPosition(): LineMarkerRendererEx.Position = LineMarkerRendererEx.Position.CUSTOM

    override fun paint(editor: Editor, g: Graphics, r: Rectangle) {
        val gutter = (editor as EditorEx).gutterComponentEx
        val barWidth = JBUIScale.scale(BAR_WIDTH)
        val x = (gutter.iconAreaOffset - barWidth).coerceAtLeast(gutter.lineMarkerAreaOffset)
        g.color = SUGGESTION_BAR_COLOR
        g.fillRect(x, r.y, barWidth, r.height)
    }

    override fun getTooltipText(): String = GiteaBundle.message("pull.request.action.create.suggestion")
    override fun canDoAction(editor: Editor, e: MouseEvent): Boolean = true
    override fun doAction(editor: Editor, e: MouseEvent) = onClick()
}
