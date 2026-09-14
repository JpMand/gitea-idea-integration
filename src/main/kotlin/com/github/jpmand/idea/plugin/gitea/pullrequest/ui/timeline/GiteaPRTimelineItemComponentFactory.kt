package com.github.jpmand.idea.plugin.gitea.pullrequest.ui.timeline

import com.github.jpmand.idea.plugin.gitea.api.models.GiteaReviewState
import com.github.jpmand.idea.plugin.gitea.api.models.GiteaReviewThread
import com.github.jpmand.idea.plugin.gitea.api.models.GiteaTimelineItem
import com.github.jpmand.idea.plugin.gitea.api.models.GiteaUser
import com.github.jpmand.idea.plugin.gitea.util.GiteaBundle
import com.intellij.collaboration.messages.CollaborationToolsBundle
import com.intellij.collaboration.ui.CollaborationToolsUIUtil
import com.intellij.collaboration.ui.HorizontalListPanel
import com.intellij.collaboration.ui.SimpleHtmlPane
import com.intellij.collaboration.ui.VerticalListPanel
import com.intellij.collaboration.ui.codereview.CodeReviewChatItemUIUtil
import com.intellij.collaboration.ui.codereview.CodeReviewChatItemUIUtil.ComponentType
import com.intellij.collaboration.ui.codereview.CodeReviewTimelineUIUtil
import com.intellij.collaboration.ui.codereview.timeline.StatusMessageComponentFactory
import com.intellij.collaboration.ui.codereview.timeline.StatusMessageType
import com.intellij.collaboration.ui.codereview.timeline.TimelineDiffComponentFactory
import com.intellij.collaboration.ui.codereview.timeline.thread.TimelineThreadCommentsPanel
import com.intellij.collaboration.ui.icon.IconsProvider
import com.intellij.diff.util.LineRange
import com.intellij.icons.AllIcons
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.application.EDT
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.diff.impl.patch.PatchHunkUtil
import com.intellij.openapi.diff.impl.patch.PatchReader
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.ui.CollectionListModel
import com.intellij.ui.PopupHandler
import com.intellij.ui.components.ActionLink
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.UIUtil
import icons.CollaborationToolsIcons
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.datatransfer.StringSelection
import javax.swing.JComponent
import javax.swing.JEditorPane

/**
 * Renders one [GiteaPRTimelineItemViewModel] using the platform timeline-item shell
 * ([CodeReviewChatItemUIUtil.build] / [CodeReviewTimelineUIUtil.createTitleTextPane] /
 * [StatusMessageComponentFactory] / [TimelineThreadCommentsPanel]) — the same building blocks the
 * bundled GitLab/GitHub timeline factories use.
 */
@Suppress("UnstableApiUsage")
class GiteaPRTimelineItemComponentFactory(
    private val project: Project,
    private val avatars: IconsProvider<GiteaUser>,
    /** Renders a body to sanitized HTML via the server; null on failure (keep the fallback). */
    private val renderMarkdown: suspend (String) -> String?,
    /** The PR's current head SHA — a thread's anchor comment carrying a different
     * [com.github.jpmand.idea.plugin.gitea.api.models.GiteaReviewComment.commitId] means the diff
     * it was anchored to is no longer the latest one, i.e. it's "outdated" (GitHub's term). */
    private val headSha: String,
) {

    fun create(cs: CoroutineScope, item: GiteaPRTimelineItemViewModel): JComponent = when (item) {
        is GiteaPRTimelineItemViewModel.Comment -> comment(cs, item)
        is GiteaPRTimelineItemViewModel.Review -> review(cs, item)
        is GiteaPRTimelineItemViewModel.Commits -> commits(item)
        is GiteaPRTimelineItemViewModel.Event -> event(item)
    }

    // ── item kinds ─────────────────────────────────────────────────────────

    private fun comment(cs: CoroutineScope, item: GiteaPRTimelineItemViewModel.Comment): JComponent {
        val pane = SimpleHtmlPane(bodyHtml(item.body))
        item.body?.let { renderMarkdownInto(cs, pane, it) }
        return chatItem(item, pane,
            urlActions(item.htmlUrl, "pull.request.action.open.comment.in.browser", "pull.request.action.copy.comment.link"))
    }

    private fun review(cs: CoroutineScope, item: GiteaPRTimelineItemViewModel.Review): JComponent {
        val content = VerticalListPanel(4).apply {
            add(reviewStateChip(item.state))
            if (!item.body.isNullOrBlank()) {
                val pane = SimpleHtmlPane(bodyHtml(item.body))
                renderMarkdownInto(cs, pane, item.body)
                add(pane)
            }
            item.threads.forEach { thread -> add(threadPanel(cs, thread)) }
        }
        return chatItem(item, content,
            urlActions(item.htmlUrl, "pull.request.action.open.comment.in.browser", "pull.request.action.copy.comment.link"))
    }

    /** Renders [markdown] and swaps it into [pane] on the EDT; leaves the fallback on failure. */
    private fun renderMarkdownInto(cs: CoroutineScope, pane: JEditorPane, markdown: String) {
        if (markdown.isBlank()) return
        cs.launch {
            val html = renderMarkdown(markdown) ?: return@launch
            withContext(Dispatchers.EDT) { pane.text = html }
        }
    }

    private fun commits(item: GiteaPRTimelineItemViewModel.Commits): JComponent {
        val list = VerticalListPanel(2).apply {
            item.commits.forEach { c ->
                add(ActionLink("${c.shortSha}  ${c.messageTitle}") { c.htmlUrl?.let(BrowserUtil::browse) })
            }
        }
        val header = JBLabel(
            GiteaBundle.message(
                if (item.commits.size == 1) "pull.request.timeline.commit.added.one"
                else "pull.request.timeline.commit.added.many",
                item.commits.size,
            ),
        )
        return CodeReviewChatItemUIUtil.build(
            ComponentType.FULL,
            { AllIcons.Vcs.Branch },
            VerticalListPanel(4).apply { add(header); add(list) },
        ) {
            withHeader(
                CodeReviewTimelineUIUtil.createTitleTextPane(actorName(item.actor), item.actor?.htmlUrl, item.timestamp),
                null,
            )
        }
    }

    private fun event(item: GiteaPRTimelineItemViewModel.Event): JComponent {
        val text = "<b>${esc(actorName(item.actor))}</b> ${esc(eventText(item))}"
        return StatusMessageComponentFactory.create(SimpleHtmlPane(text), StatusMessageType.SECONDARY_INFO)
    }

    // ── shell + helpers ────────────────────────────────────────────────────

    private fun chatItem(
        item: GiteaPRTimelineItemViewModel,
        content: JComponent,
        actions: List<AnAction>,
    ): JComponent {
        if (actions.isNotEmpty()) {
            PopupHandler.installPopupMenu(content, DefaultActionGroup(actions), "GiteaPRTimelinePopup")
        }
        return CodeReviewChatItemUIUtil.build(
            ComponentType.FULL,
            { size -> avatars.getIcon(item.actor, size) },
            content,
        ) {
            withHeader(
                CodeReviewTimelineUIUtil.createTitleTextPane(actorName(item.actor), item.actor?.htmlUrl, item.timestamp),
                null,
            )
        }
    }

    /**
     * Renders a review thread like GitHub's inline-comment cards: the file:line location, the
     * surrounding diff-hunk context (from the anchor comment's
     * [com.github.jpmand.idea.plugin.gitea.api.models.GiteaReviewComment.diffHunk]) so the
     * comment doesn't require opening the diff viewer to understand, an "Outdated" badge when the
     * anchor's [com.github.jpmand.idea.plugin.gitea.api.models.GiteaReviewComment.commitId] no
     * longer matches [headSha], then the comments themselves.
     */
    private fun threadPanel(cs: CoroutineScope, thread: GiteaReviewThread): JComponent {
        val anchor = thread.comments.firstOrNull()
        val isOutdated = anchor?.commitId != null && anchor.commitId != headSha

        val location = buildString {
            append(thread.path ?: "")
            (thread.newLine ?: thread.oldLine)?.let { append(":").append(it) }
        }
        val locationRow = HorizontalListPanel(6).apply {
            add(JBLabel(location).apply {
                foreground = UIUtil.getContextHelpForeground()
                font = JBFont.small()
            })
            if (isOutdated) {
                // Reuses the platform's own tag component + bundled string — the same "OUTDATED"
                // pill the bundled GitHub plugin's review-thread timeline shows.
                add(CollaborationToolsUIUtil.createTagLabel(CollaborationToolsBundle.message("review.thread.outdated.tag")))
            }
        }
        val commentsPanel = TimelineThreadCommentsPanel(
            CollectionListModel(thread.comments),
            { c -> JBLabel("<html><b>${esc(c.author?.login ?: "")}</b>: ${esc(c.body ?: "")}</html>") },
        )
        return VerticalListPanel(2).apply {
            add(locationRow)
            diffHunkComponent(cs, thread.path, anchor?.diffHunk)?.let { add(it) }
            add(commentsPanel)
        }
    }

    /**
     * A real, syntax-highlighted, theme-consistent diff preview of the anchor comment's diff
     * hunk — reuses the platform's [TimelineDiffComponentFactory] (the same editor-based renderer
     * the bundled GitHub plugin's review-thread timeline uses) instead of hand-rolling one.
     * `diffHunk` is Gitea's raw unified-diff hunk text; [PatchHunkUtil.createPatchFromHunk] wraps
     * it with synthetic `--- a/`/`+++ b/` headers so [PatchReader] can parse it as a one-file
     * patch (same trick `GHPRTimelineThreadViewModel.calcDiffWithAnchor` uses for GitHub's
     * `diff_hunk`). Returns `null` when there's no hunk, no path, or it fails to parse (e.g. a
     * PR-level, non-line-anchored review comment) — no diff box shown in that case.
     *
     * The commented line itself is highlighted: like GitHub's `diff_hunk`, Gitea's always ends
     * exactly at the commented line (verified against the swagger-documented behavior, mirrored
     * by `calcDiffWithAnchor`), so the anchor is simply the hunk's last line — no need to map
     * [com.github.jpmand.idea.plugin.gitea.api.models.GiteaReviewComment.newLine]/`oldLine`
     * through [PatchHunkUtil.findHunkLineIndex] to locate it.
     */
    private fun diffHunkComponent(cs: CoroutineScope, path: String?, diffHunk: String?): JComponent? {
        if (diffHunk.isNullOrBlank() || path.isNullOrBlank()) return null
        val hunk = try {
            PatchReader(PatchHunkUtil.createPatchFromHunk(path, diffHunk)).readTextPatches().firstOrNull()?.hunks?.firstOrNull()
        } catch (e: Exception) {
            thisLogger().warn("Failed to parse diff hunk for $path", e)
            null
        } ?: return null
        if (hunk.lines.isEmpty()) return null

        // Bounds leading context to DIFF_CONTEXT_SIZE lines before the anchor, matching the
        // platform's own default (can't reference TimelineDiffComponentFactory.DIFF_CONTEXT_SIZE
        // directly — it's @ApiStatus.Internal).
        val truncatedHunk = PatchHunkUtil.truncateHunkBefore(hunk, hunk.lines.lastIndex - DIFF_CONTEXT_SIZE)
        val anchorRange = LineRange(truncatedHunk.lines.lastIndex, truncatedHunk.lines.size)

        val diffComponent = TimelineDiffComponentFactory.createDiffComponentIn(
            cs, project, EditorFactory.getInstance(), truncatedHunk, anchorRange,
        )
        return TimelineDiffComponentFactory.createDiffWithHeader(cs, path, flowOf(null), diffComponent)
    }

    private fun reviewStateChip(state: GiteaReviewState): JComponent {
        val (icon, key) = when (state) {
            GiteaReviewState.APPROVED ->
                com.intellij.icons.AllIcons.RunConfigurations.TestPassed to "pull.request.timeline.review.approved"
            GiteaReviewState.REQUEST_CHANGES ->
                com.intellij.icons.AllIcons.General.Warning to "pull.request.timeline.review.changes"
            else -> CollaborationToolsIcons.Review.CommentUnread to "pull.request.timeline.review.commented"
        }
        return JBLabel(GiteaBundle.message(key), icon, JBLabel.LEADING)
    }

    private fun urlActions(url: String?, openKey: String, copyKey: String): List<AnAction> {
        if (url == null) return emptyList()
        return listOf(
            simpleAction(openKey) { BrowserUtil.browse(url) },
            simpleAction(copyKey) { CopyPasteManager.getInstance().setContents(StringSelection(url)) },
        )
    }

    private fun simpleAction(bundleKey: String, run: () -> Unit): AnAction =
        object : AnAction(GiteaBundle.message(bundleKey)) {
            override fun getActionUpdateThread() = ActionUpdateThread.BGT
            override fun actionPerformed(e: AnActionEvent) = run()
        }

    private fun eventText(item: GiteaPRTimelineItemViewModel.Event): String = when (item.kind) {
        GiteaTimelineItem.Event.Kind.CLOSED -> GiteaBundle.message("pull.request.timeline.event.closed")
        GiteaTimelineItem.Event.Kind.REOPENED -> GiteaBundle.message("pull.request.timeline.event.reopened")
        GiteaTimelineItem.Event.Kind.MERGED -> GiteaBundle.message("pull.request.timeline.event.merged")
        GiteaTimelineItem.Event.Kind.LABEL_ADDED ->
            GiteaBundle.message("pull.request.timeline.event.label.added", item.label?.name ?: "")
        GiteaTimelineItem.Event.Kind.LABEL_REMOVED ->
            GiteaBundle.message("pull.request.timeline.event.label.removed", item.label?.name ?: "")
        GiteaTimelineItem.Event.Kind.MILESTONE_CHANGED -> GiteaBundle.message("pull.request.timeline.event.milestone.changed")
        GiteaTimelineItem.Event.Kind.ASSIGNED -> GiteaBundle.message("pull.request.timeline.event.assigned")
        GiteaTimelineItem.Event.Kind.UNASSIGNED -> GiteaBundle.message("pull.request.timeline.event.unassigned")
        GiteaTimelineItem.Event.Kind.REVIEW_REQUESTED ->
            GiteaBundle.message("pull.request.timeline.event.review.requested", item.user?.login ?: "")
        GiteaTimelineItem.Event.Kind.REVIEW_REQUEST_REMOVED ->
            GiteaBundle.message("pull.request.timeline.event.review.request.removed", item.user?.login ?: "")
        GiteaTimelineItem.Event.Kind.REVIEW_DISMISSED -> GiteaBundle.message("pull.request.timeline.event.review.dismissed")
        GiteaTimelineItem.Event.Kind.TITLE_CHANGED -> GiteaBundle.message("pull.request.timeline.event.title.changed")
        GiteaTimelineItem.Event.Kind.BASE_BRANCH_CHANGED ->
            GiteaBundle.message("pull.request.timeline.event.base.changed", item.oldValue ?: "", item.newValue ?: "")
        GiteaTimelineItem.Event.Kind.HEAD_BRANCH_DELETED -> GiteaBundle.message("pull.request.timeline.event.head.deleted")
        GiteaTimelineItem.Event.Kind.LOCKED -> GiteaBundle.message("pull.request.timeline.event.locked")
        GiteaTimelineItem.Event.Kind.UNLOCKED -> GiteaBundle.message("pull.request.timeline.event.unlocked")

        GiteaTimelineItem.Event.Kind.REFERENCED_FROM_ISSUE ->
            GiteaBundle.message("pull.request.timeline.event.referenced.from.issue", item.newValue ?: "")
        GiteaTimelineItem.Event.Kind.REFERENCED_FROM_PULL_REQUEST ->
            GiteaBundle.message("pull.request.timeline.event.referenced.from.pull", item.newValue ?: "")
        GiteaTimelineItem.Event.Kind.REFERENCED_FROM_COMMENT ->
            GiteaBundle.message("pull.request.timeline.event.referenced.from.comment", item.newValue ?: "")
        GiteaTimelineItem.Event.Kind.REFERENCED_FROM_COMMIT ->
            GiteaBundle.message("pull.request.timeline.event.referenced.from.commit", item.newValue ?: "")

        GiteaTimelineItem.Event.Kind.TIME_TRACKING_STARTED -> GiteaBundle.message("pull.request.timeline.event.time.tracking.started")
        GiteaTimelineItem.Event.Kind.TIME_TRACKING_STOPPED -> GiteaBundle.message("pull.request.timeline.event.time.tracking.stopped")
        GiteaTimelineItem.Event.Kind.TIME_ADDED_MANUALLY -> GiteaBundle.message("pull.request.timeline.event.time.added.manually")
        GiteaTimelineItem.Event.Kind.TIME_TRACKING_CANCELLED -> GiteaBundle.message("pull.request.timeline.event.time.tracking.cancelled")
        GiteaTimelineItem.Event.Kind.TIME_ESTIMATE_CHANGED -> GiteaBundle.message("pull.request.timeline.event.time.estimate.changed")
        GiteaTimelineItem.Event.Kind.DUE_DATE_ADDED -> GiteaBundle.message("pull.request.timeline.event.due.date.added")
        GiteaTimelineItem.Event.Kind.DUE_DATE_MODIFIED -> GiteaBundle.message("pull.request.timeline.event.due.date.modified")
        GiteaTimelineItem.Event.Kind.DUE_DATE_REMOVED -> GiteaBundle.message("pull.request.timeline.event.due.date.removed")
        GiteaTimelineItem.Event.Kind.DEPENDENCY_ADDED -> GiteaBundle.message("pull.request.timeline.event.dependency.added")
        GiteaTimelineItem.Event.Kind.DEPENDENCY_REMOVED -> GiteaBundle.message("pull.request.timeline.event.dependency.removed")
        GiteaTimelineItem.Event.Kind.PROJECT_CHANGED -> GiteaBundle.message("pull.request.timeline.event.project.changed")
        GiteaTimelineItem.Event.Kind.PROJECT_COLUMN_CHANGED -> GiteaBundle.message("pull.request.timeline.event.project.column.changed")
        GiteaTimelineItem.Event.Kind.PINNED -> GiteaBundle.message("pull.request.timeline.event.pinned")
        GiteaTimelineItem.Event.Kind.UNPINNED -> GiteaBundle.message("pull.request.timeline.event.unpinned")
        GiteaTimelineItem.Event.Kind.AUTO_MERGE_SCHEDULED -> GiteaBundle.message("pull.request.timeline.event.auto.merge.scheduled")
        GiteaTimelineItem.Event.Kind.AUTO_MERGE_CANCELLED -> GiteaBundle.message("pull.request.timeline.event.auto.merge.cancelled")
    }

    private fun actorName(user: GiteaUser?): String = user?.let { it.fullName ?: it.login } ?: "—"

    private fun bodyHtml(body: String?): String =
        if (body.isNullOrBlank()) "<i>${esc(GiteaBundle.message("pull.request.timeline.no.body"))}</i>"
        else esc(body).replace("\n", "<br>")

    private fun esc(s: String): String = StringUtil.escapeXmlEntities(s)

    private companion object {
        const val DIFF_CONTEXT_SIZE = 3
    }
}
