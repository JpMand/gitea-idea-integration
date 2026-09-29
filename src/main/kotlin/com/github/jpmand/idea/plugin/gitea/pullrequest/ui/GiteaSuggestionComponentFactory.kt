package com.github.jpmand.idea.plugin.gitea.pullrequest.ui

import com.github.jpmand.idea.plugin.gitea.pullrequest.review.GiteaSuggestion
import com.github.jpmand.idea.plugin.gitea.pullrequest.review.applySuggestion
import com.github.jpmand.idea.plugin.gitea.util.GiteaBundle
import com.intellij.collaboration.ui.VerticalListPanel
import com.intellij.collaboration.ui.codereview.comment.CodeReviewCommentUIUtil
import com.intellij.collaboration.ui.codereview.timeline.TimelineDiffComponentFactory
import com.intellij.openapi.diff.impl.patch.PatchHunk
import com.intellij.openapi.diff.impl.patch.PatchLine
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.panels.Wrapper
import com.intellij.util.ui.GraphicsUtil
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.awt.BorderLayout
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.geom.RoundRectangle2D
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel

/**
 * The "Suggested change" box: the suggestion's diff under a small header, laid out like the GitHub
 * plugin's. Shared between [createSuggestionComponent] (a posted comment, with Apply in the header)
 * and the suggested-change composer (no Apply yet — nothing's been posted), so both show the same
 * rendered diff instead of ever exposing the raw marker+fence text a [GiteaSuggestion] is encoded as.
 */
@Suppress("UnstableApiUsage")
fun createSuggestionDiffBox(
    cs: CoroutineScope,
    project: Project,
    suggestion: GiteaSuggestion,
    /** Adds an "Apply suggestion" button to the box's header; `null` for the composer's preview. */
    onApply: (() -> Unit)? = null,
): JComponent {
    val hunk = PatchHunk(
        suggestion.oldStartLine,
        suggestion.oldStartLine + suggestion.oldLines.size,
        suggestion.oldStartLine,
        suggestion.oldStartLine + suggestion.newLines.size,
    ).apply {
        suggestion.oldLines.forEach { addLine(PatchLine(PatchLine.Type.REMOVE, it)) }
        suggestion.newLines.forEach { addLine(PatchLine(PatchLine.Type.ADD, it)) }
    }
    val diffComponent = TimelineDiffComponentFactory.createDiffComponentIn(cs, project, EditorFactory.getInstance(), hunk, null)
    // The GitHub plugin's suggested-change box: a grey title with the Apply button on the right,
    // over the diff, in a rounded comment-bubble frame. The platform's diff-with-header box is made
    // for file names (it showed "Suggested change" as a greyed-out file link with an unknown-file icon).
    val header = JPanel(BorderLayout()).apply {
        isOpaque = false
        border = JBUI.Borders.compound(
            JBUI.Borders.customLineBottom(CodeReviewCommentUIUtil.COMMENT_BUBBLE_BORDER_COLOR),
            JBUI.Borders.empty(4, 8),
        )
        add(JBLabel(GiteaBundle.message("pull.request.suggestion.header")).apply {
            foreground = UIUtil.getContextHelpForeground()
        }, BorderLayout.WEST)
        if (onApply != null) {
            add(JButton(GiteaBundle.message("pull.request.action.apply.suggestion")).apply {
                isOpaque = false
                addActionListener { onApply() }
            }, BorderLayout.EAST)
        }
    }
    return RoundedBubblePanel().apply {
        add(header, BorderLayout.NORTH)
        add(diffComponent, BorderLayout.CENTER)
    }
}

/**
 * A panel framed by the platform's comment-bubble border colour with rounded corners, clipping its
 * children to them (the platform's own rounded panel is internal API).
 */
private class RoundedBubblePanel : JPanel(BorderLayout()) {
    private val arc get() = JBUI.scale(8)

    init {
        isOpaque = false
        border = JBUI.Borders.empty(1)
    }

    // Children repaint through this panel, so the clip applies to them too.
    override fun isPaintingOrigin(): Boolean = true

    override fun paintChildren(g: Graphics) {
        val g2 = g.create() as Graphics2D
        try {
            g2.clip(RoundRectangle2D.Float(0f, 0f, width.toFloat(), height.toFloat(), arc.toFloat(), arc.toFloat()))
            super.paintChildren(g2)
        } finally {
            g2.dispose()
        }
        // Drawn after the children, so their corners never cover it.
        val g3 = g.create() as Graphics2D
        try {
            GraphicsUtil.setupAAPainting(g3)
            g3.color = CodeReviewCommentUIUtil.COMMENT_BUBBLE_BORDER_COLOR
            g3.draw(RoundRectangle2D.Float(0.5f, 0.5f, width - 1f, height - 1f, arc.toFloat(), arc.toFloat()))
        } finally {
            g3.dispose()
        }
    }
}

/**
 * [createSuggestionDiffBox] with its "Apply suggestion" button, for an already-posted comment; the
 * composer (nothing posted yet, so nothing to apply) uses [createSuggestionDiffBox] directly.
 */
@Suppress("UnstableApiUsage")
fun createSuggestionComponent(cs: CoroutineScope, project: Project, suggestion: GiteaSuggestion, onApply: () -> Unit): JComponent =
    Wrapper(createSuggestionDiffBox(cs, project, suggestion, onApply)).apply {
        isOpaque = false
        border = JBUI.Borders.empty(4, 0)
    }

/**
 * Combines a comment's already-rendered body with its "Suggested change" box (or returns
 * [bodyComponent] unchanged when [suggestion] or [path] is `null`) — shared so the Timeline and
 * diff-editor comment factories render a suggestion identically. [bodyComponent] is omitted
 * entirely when [bodyIsBlank] — the whole comment was just the suggestion, no explanation text of
 * its own.
 */
@Suppress("UnstableApiUsage")
fun withSuggestion(
    cs: CoroutineScope,
    project: Project,
    path: String?,
    bodyComponent: JComponent,
    bodyIsBlank: Boolean,
    suggestion: GiteaSuggestion?,
): JComponent {
    if (suggestion == null || path == null) return bodyComponent
    return VerticalListPanel(4).apply {
        if (!bodyIsBlank) add(bodyComponent)
        add(createSuggestionComponent(cs, project, suggestion) { cs.launch { applySuggestion(cs, project, path, suggestion) } })
    }
}
