package com.github.jpmand.idea.plugin.gitea.pullrequest.ui.timeline

import com.github.jpmand.idea.plugin.gitea.api.models.GiteaReviewState
import com.github.jpmand.idea.plugin.gitea.api.models.GiteaReviewThread
import com.github.jpmand.idea.plugin.gitea.api.models.GiteaTimelineItem
import com.github.jpmand.idea.plugin.gitea.api.models.GiteaUser
import com.github.jpmand.idea.plugin.gitea.util.GiteaBundle
import com.intellij.collaboration.ui.HorizontalListPanel
import com.intellij.collaboration.ui.SimpleHtmlPane
import com.intellij.collaboration.ui.VerticalListPanel
import com.intellij.collaboration.ui.codereview.CodeReviewChatItemUIUtil
import com.intellij.collaboration.ui.codereview.CodeReviewChatItemUIUtil.ComponentType
import com.intellij.collaboration.ui.codereview.CodeReviewTimelineUIUtil
import com.intellij.collaboration.ui.codereview.timeline.StatusMessageComponentFactory
import com.intellij.collaboration.ui.codereview.timeline.StatusMessageType
import com.intellij.collaboration.ui.codereview.timeline.thread.TimelineThreadCommentsPanel
import com.intellij.collaboration.ui.icon.IconsProvider
import com.intellij.icons.AllIcons
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.application.EDT
import com.intellij.ui.CollectionListModel
import com.intellij.ui.JBColor
import com.intellij.ui.PopupHandler
import com.intellij.ui.components.ActionLink
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import icons.CollaborationToolsIcons
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.Font
import java.awt.datatransfer.StringSelection
import javax.swing.JComponent
import javax.swing.JEditorPane
import javax.swing.JTextPane
import javax.swing.text.SimpleAttributeSet
import javax.swing.text.StyleConstants

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
            item.threads.forEach { thread -> add(threadPanel(thread)) }
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
    private fun threadPanel(thread: GiteaReviewThread): JComponent {
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
                add(JBLabel(GiteaBundle.message("pull.request.timeline.thread.outdated")).apply {
                    foreground = JBColor.ORANGE
                    font = JBFont.small().asBold()
                })
            }
        }
        val commentsPanel = TimelineThreadCommentsPanel(
            CollectionListModel(thread.comments),
            { c -> JBLabel("<html><b>${esc(c.author?.login ?: "")}</b>: ${esc(c.body ?: "")}</html>") },
        )
        return VerticalListPanel(2).apply {
            add(locationRow)
            diffHunkPanel(anchor?.diffHunk)?.let { add(it) }
            add(commentsPanel)
        }
    }

    /** A small syntax-colored preview of the anchor comment's diff hunk — additions/deletions
     * get GitHub-style backgrounds, `@@ ... @@` headers are muted. `null` when there's no hunk
     * (e.g. a PR-level, non-line-anchored review comment). */
    private fun diffHunkPanel(diffHunk: String?): JComponent? {
        if (diffHunk.isNullOrBlank()) return null
        val pane = JTextPane().apply {
            isEditable = false
            font = Font(Font.MONOSPACED, Font.PLAIN, JBFont.small().size)
            border = JBUI.Borders.empty(4, 6)
        }
        val doc = pane.styledDocument
        val header = SimpleAttributeSet().apply {
            StyleConstants.setForeground(this, DIFF_HEADER_FG)
            StyleConstants.setItalic(this, true)
        }
        val addition = SimpleAttributeSet().apply { StyleConstants.setBackground(this, DIFF_ADD_BG) }
        val deletion = SimpleAttributeSet().apply { StyleConstants.setBackground(this, DIFF_DEL_BG) }
        runCatching {
            diffHunk.trimEnd('\n').lineSequence().forEach { line ->
                val attrs = when {
                    line.startsWith("@@") -> header
                    line.startsWith("+") -> addition
                    line.startsWith("-") -> deletion
                    else -> null
                }
                doc.insertString(doc.length, line + "\n", attrs)
            }
        }
        return pane
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
        GiteaTimelineItem.Event.Kind.REFERENCED -> GiteaBundle.message("pull.request.timeline.event.referenced")
    }

    private fun actorName(user: GiteaUser?): String = user?.let { it.fullName ?: it.login } ?: "—"

    private fun bodyHtml(body: String?): String =
        if (body.isNullOrBlank()) "<i>${esc(GiteaBundle.message("pull.request.timeline.no.body"))}</i>"
        else esc(body).replace("\n", "<br>")

    private fun esc(s: String): String = StringUtil.escapeXmlEntities(s)

    private companion object {
        val DIFF_ADD_BG = JBColor(0xE6FFEC, 0x253B2D)
        val DIFF_DEL_BG = JBColor(0xFFEBE9, 0x3B2526)
        val DIFF_HEADER_FG = JBColor(0x6E7781, 0x8B949E)
    }
}
