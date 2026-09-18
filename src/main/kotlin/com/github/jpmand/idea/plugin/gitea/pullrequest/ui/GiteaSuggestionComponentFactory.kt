package com.github.jpmand.idea.plugin.gitea.pullrequest.ui

import com.github.jpmand.idea.plugin.gitea.pullrequest.review.GiteaSuggestion
import com.github.jpmand.idea.plugin.gitea.util.GiteaBundle
import com.intellij.collaboration.ui.VerticalListPanel
import com.intellij.collaboration.ui.codereview.timeline.TimelineDiffComponentFactory
import com.intellij.openapi.diff.impl.patch.PatchHunk
import com.intellij.openapi.diff.impl.patch.PatchLine
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.project.Project
import com.intellij.ui.components.ActionLink
import com.intellij.util.ui.JBUI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.flowOf
import javax.swing.JComponent

/**
 * A "Suggested change" box — the exact same diff-box chrome an ordinary comment's diff-hunk
 * preview already uses
 * ([com.github.jpmand.idea.plugin.gitea.pullrequest.ui.timeline.GiteaPRTimelineItemComponentFactory.diffHunkComponent],
 * both built on [TimelineDiffComponentFactory.createDiffWithHeader]/`createDiffComponentIn`), with
 * an "Apply suggestion" row below it — matching how "Resolve conversation"/"Reply" are already
 * their own rows in this codebase, not crammed into the box's header — instead of showing the raw
 * marker+fence text a [GiteaSuggestion] is encoded as.
 */
@Suppress("UnstableApiUsage")
fun createSuggestionComponent(cs: CoroutineScope, project: Project, suggestion: GiteaSuggestion, onApply: () -> Unit): JComponent {
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
    val diffBox = TimelineDiffComponentFactory.createDiffWithHeader(
        cs, GiteaBundle.message("pull.request.suggestion.header"), flowOf(null), diffComponent,
    )

    return VerticalListPanel(4).apply {
        border = JBUI.Borders.empty(4, 0)
        add(diffBox)
        add(ActionLink(GiteaBundle.message("pull.request.action.apply.suggestion")) { onApply() })
    }
}

/**
 * Combines a comment's already-rendered body with its "Suggested change" box (or returns
 * [bodyComponent] unchanged when [suggestion] is `null`) — shared so the Timeline and diff-editor
 * comment factories render a suggestion identically. [bodyComponent] is omitted entirely when
 * [bodyIsBlank] — the whole comment was just the suggestion, no explanation text of its own.
 * Apply logic isn't wired up yet (TODO); the action currently just says so
 * ([applySuggestionNotImplementedYet]).
 */
@Suppress("UnstableApiUsage")
fun withSuggestion(
    cs: CoroutineScope,
    project: Project,
    bodyComponent: JComponent,
    bodyIsBlank: Boolean,
    suggestion: GiteaSuggestion?,
): JComponent {
    if (suggestion == null) return bodyComponent
    return VerticalListPanel(4).apply {
        if (!bodyIsBlank) add(bodyComponent)
        add(createSuggestionComponent(cs, project, suggestion) { applySuggestionNotImplementedYet(project, suggestion) })
    }
}
