package com.github.jpmand.idea.plugin.gitea.pullrequest.ui.timeline

import com.github.jpmand.idea.plugin.gitea.api.models.*
import com.github.jpmand.idea.plugin.gitea.pullrequest.review.GiteaSuggestionUtil
import com.github.jpmand.idea.plugin.gitea.pullrequest.ui.comment.GiteaPRCommentFieldFactory
import com.github.jpmand.idea.plugin.gitea.pullrequest.ui.comment.GiteaPRSubmittableTextViewModel
import com.github.jpmand.idea.plugin.gitea.pullrequest.ui.createThreadCommentsPanel
import com.github.jpmand.idea.plugin.gitea.pullrequest.ui.withSuggestion
import com.github.jpmand.idea.plugin.gitea.util.GiteaBundle
import com.intellij.collaboration.messages.CollaborationToolsBundle
import com.intellij.collaboration.ui.*
import com.intellij.collaboration.ui.codereview.CodeReviewChatItemUIUtil
import com.intellij.collaboration.ui.codereview.CodeReviewChatItemUIUtil.ComponentType
import com.intellij.collaboration.ui.codereview.CodeReviewTimelineUIUtil
import com.intellij.collaboration.ui.codereview.comment.CodeReviewCommentUIUtil
import com.intellij.collaboration.ui.codereview.comment.CodeReviewSubmittableTextViewModelBase
import com.intellij.collaboration.ui.codereview.comment.CodeReviewTextEditingViewModel
import com.intellij.collaboration.ui.codereview.timeline.StatusMessageComponentFactory
import com.intellij.collaboration.ui.codereview.timeline.StatusMessageType
import com.intellij.collaboration.ui.codereview.timeline.TimelineDiffComponentFactory
import com.intellij.collaboration.ui.icon.IconsProvider
import com.intellij.diff.util.LineRange
import com.intellij.icons.AllIcons
import com.intellij.ide.BrowserUtil
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
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
import com.intellij.ui.ColorUtil
import com.intellij.ui.JBColor
import com.intellij.ui.PopupHandler
import com.intellij.ui.RoundedLineBorder
import com.intellij.ui.components.ActionLink
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.panels.Wrapper
import com.intellij.util.text.DateFormatUtil
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import java.awt.datatransfer.StringSelection
import java.util.*
import javax.swing.JButton
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
    /** The signed-in account's login — gates the edit/delete controls to a comment's own author
     * (Gitea's API exposes no `viewerCanUpdate`-style flag, so this is a client-side check). */
    private val currentUserLogin: String,
    private val onEditComment: suspend (id: Long, body: String) -> Unit,
    private val onDeleteComment: suspend (id: Long) -> Unit,
    /** Requests the ToolWindow's Details tab open (or focus) with the given commit's changes
     * selected in the changes tree — used by both the "added N commits" block and a
     * "referenced from commit" event, instead of opening the commit in a browser. */
    private val onOpenCommit: (sha: String) -> Unit,
    /** Replies to an existing review-comment thread, identified by its anchor comment id. */
    private val onReplyToThread: suspend (threadId: Long, body: String) -> Unit,
    /** Resolves/unresolves a review-comment thread, identified by its anchor comment id. */
    private val onResolveThread: suspend (threadId: Long) -> Unit,
    private val onUnresolveThread: suspend (threadId: Long) -> Unit,
    /** The signed-in account's own profile — used for the reply composer's avatar; `null` while
     * still loading (the composer stays hidden until it resolves). */
    private val currentUser: StateFlow<GiteaUser?>,
    private val mentionCandidates: StateFlow<List<GiteaUser>>,
) {

    fun create(cs: CoroutineScope, item: GiteaPRTimelineItemViewModel): JComponent = when (item) {
        is GiteaPRTimelineItemViewModel.Comment -> comment(cs, item)
        is GiteaPRTimelineItemViewModel.Review -> review(cs, item)
        is GiteaPRTimelineItemViewModel.Commits -> commits(item)
        is GiteaPRTimelineItemViewModel.Event -> event(item)
    }

    // ── item kinds ─────────────────────────────────────────────────────────

    private fun comment(cs: CoroutineScope, item: GiteaPRTimelineItemViewModel.Comment): JComponent {
        val (pane, actionsPanel) = commentBodyAndActions(cs, item.id, item.actor?.login, item.body)
        return chatItem(item, pane,
            urlActions(item.htmlUrl, "pull.request.action.open.comment.in.browser", "pull.request.action.copy.comment.link"),
            actionsPanel, edited = item.edited)
    }

    /**
     * A review's verdict (Comment/Approved/Request Changes) plus its inline comment threads are
     * rendered as one visually grouped, colored unit — a border tinted to the verdict, matching
     * [reviewStateChip]'s accent bar — so it reads as clearly distinct from a plain top-level
     * comment or a non-comment activity event, not just indentation.
     */
    private fun review(cs: CoroutineScope, item: GiteaPRTimelineItemViewModel.Review): JComponent {
        val statusType = reviewStatusType(item.state)
        val content = VerticalListPanel(4).apply {
            add(reviewStateLabel(item.state))
            if (!item.body.isNullOrBlank()) {
                val pane = SimpleHtmlPane(bodyHtml(item.body))
                renderMarkdownInto(cs, pane, item.body)
                add(pane)
            }
            item.threads.forEach { thread -> add(threadPanel(cs, thread)) }
            border = JBUI.Borders.compound(RoundedLineBorder(reviewAccentColor(statusType), 8, 1), JBUI.Borders.empty(8))
        }
        return chatItem(item, content,
            urlActions(item.htmlUrl, "pull.request.action.open.comment.in.browser", "pull.request.action.copy.comment.link"))
    }

    /** Renders [markdown] and swaps it into [pane] on the EDT; leaves the fallback on failure. */
    private fun renderMarkdownInto(cs: CoroutineScope, pane: JEditorPane, markdown: String) {
        if (markdown.isBlank()) return
        cs.launch {
            val html = renderMarkdown(markdown) ?: return@launch
            withContext(Dispatchers.EDT) {
                pane.text = html
                pane.contentType = "text/html"
            }
        }
    }

    private fun commits(item: GiteaPRTimelineItemViewModel.Commits): JComponent {
        val list = VerticalListPanel(4).apply {
            item.commits.forEach { c -> add(commitRow(c)) }
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
                CodeReviewTimelineUIUtil.createTitleTextPane(actorName(item.actor, item.rawActor), item.actor?.htmlUrl, item.timestamp),
                null,
            )
        }
    }

    /** One commit: hash (linked, opens the commit) + message title (plain text, not a link) on
     * the first row, committer + commit timestamp on a second — same blue accent bar the review
     * verdict's border uses for [StatusMessageType.INFO], since a commit is informational the
     * same way a plain review comment is. */
    private fun commitRow(c: GiteaTimelineItem.Commit): JComponent {
        val titleRow = HorizontalListPanel(6).apply {
            add(ActionLink(c.shortSha) { onOpenCommit(c.sha) })
            add(JBLabel(c.messageTitle))
        }
        val metaRow = JBLabel("${actorName(c.actor, c.rawAuthor)} ${DateFormatUtil.formatDateTime(c.timestamp)}").apply {
            foreground = UIUtil.getContextHelpForeground()
            font = JBFont.small()
        }
        val row = VerticalListPanel(0).apply { add(titleRow); add(metaRow) }
        return StatusMessageComponentFactory.create(row, StatusMessageType.INFO)
    }

    private fun event(item: GiteaPRTimelineItemViewModel.Event): JComponent {
        val sha = item.newValue
        if (item.kind == GiteaTimelineItem.Event.Kind.REFERENCED_FROM_COMMIT && sha != null) {
            val prefixHtml = "<b>${esc(actorName(item.actor))}</b> " +
                esc(GiteaBundle.message("pull.request.timeline.event.referenced.from.commit", "").trimEnd())
            val row = HorizontalListPanel(4).apply {
                add(SimpleHtmlPane(prefixHtml))
                add(ActionLink(sha.take(7)) { onOpenCommit(sha) })
            }
            return StatusMessageComponentFactory.create(row, StatusMessageType.SECONDARY_INFO)
        }
        val text = "<b>${esc(actorName(item.actor))}</b> ${esc(eventText(item))}"
        return StatusMessageComponentFactory.create(SimpleHtmlPane(text), StatusMessageType.SECONDARY_INFO)
    }

    // ── shell + helpers ────────────────────────────────────────────────────

    private fun chatItem(
        item: GiteaPRTimelineItemViewModel,
        content: JComponent,
        actions: List<AnAction>,
        actionsPanel: JComponent? = null,
        edited: Boolean = false,
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
                titleTextPane(actorName(item.actor), item.actor?.htmlUrl, item.timestamp, edited),
                actionsPanel,
            )
        }
    }

    /**
     * Renders a comment's body, swappable in-place for an editor when its own author clicks Edit
     * — plus, only for the signed-in user's own comments (`id <= 0`, the synthetic PR-description
     * row, never gets controls), an edit/delete actions row using the same platform helpers
     * GitHub's own comment factories use ([CodeReviewCommentUIUtil.createEditButton] /
     * [CodeReviewCommentUIUtil.createDeleteCommentIconButton] — the latter already asks for
     * confirmation before invoking its callback).
     */
    private fun commentBodyAndActions(
        cs: CoroutineScope,
        id: Long,
        authorLogin: String?,
        body: String?,
    ): Pair<JComponent, JComponent?> {
        val pane = SimpleHtmlPane(bodyHtml(body))
        body?.let { renderMarkdownInto(cs, pane, it) }
        if (id <= 0 || authorLogin != currentUserLogin) return pane to null

        val editVmFlow = MutableStateFlow<CodeReviewTextEditingViewModel?>(null)
        val bodyComponent = EditableComponentFactory.wrapTextComponent(cs, pane, editVmFlow)

        val actionsPanel = HorizontalListPanel(CodeReviewCommentUIUtil.Actions.HORIZONTAL_GAP).apply {
            add(CodeReviewCommentUIUtil.createEditButton {
                val editVm = CommentEditViewModel(cs, body.orEmpty(), id) { editVmFlow.value = null }
                editVmFlow.value = editVm
                editVm.requestFocus()
            })
            add(CodeReviewCommentUIUtil.createDeleteCommentIconButton {
                cs.launch { onDeleteComment(id) }
            })
        }
        return bodyComponent to actionsPanel
    }

    private inner class CommentEditViewModel(
        cs: CoroutineScope,
        initialText: String,
        private val commentId: Long,
        private val onDone: () -> Unit,
    ) : CodeReviewSubmittableTextViewModelBase(project, cs, initialText), CodeReviewTextEditingViewModel {
        override fun save() {
            submit { newBody ->
                onEditComment(commentId, newBody)
                onDone()
            }
        }

        override fun stopEditing() = onDone()
    }

    /**
     * Renders a review thread like GitHub's inline-comment cards: the surrounding diff-hunk
     * context (from the anchor comment's
     * [com.github.jpmand.idea.plugin.gitea.api.models.GiteaReviewComment.diffHunk]) so the
     * comment doesn't require opening the diff viewer to understand — its own header already
     * shows the file path, so no separate file:line label is needed here — an "Outdated" badge
     * when the anchor's [com.github.jpmand.idea.plugin.gitea.api.models.GiteaReviewComment.commitId]
     * no longer matches [headSha], then the comments themselves.
     */
    private fun threadPanel(cs: CoroutineScope, thread: GiteaReviewThread): JComponent {
        val anchor = thread.comments.firstOrNull()
        val isOutdated = anchor?.commitId != null && anchor.commitId != headSha

        val commentsPanel = createThreadCommentsPanel(thread.comments) { c -> threadCommentRow(cs, c) }
        return VerticalListPanel(2).apply {
            diffHunkComponent(cs, thread.path, anchor?.diffHunk)?.let { add(it) }
            if (isOutdated) {
                // Reuses the platform's own tag component + bundled string — the same "OUTDATED"
                // pill the bundled GitHub plugin's review-thread timeline shows.
                add(CollaborationToolsUIUtil.createTagLabel(CollaborationToolsBundle.message("review.thread.outdated.tag")))
            }
            add(commentsPanel)
            add(resolveRow(cs, thread).apply { border = JBUI.Borders.empty(2, 0) })
            // Outdated threads (anchored to a diff that's no longer current) can't be replied to.
            // Reply targets the thread's last comment (not the anchor) so Gitea's reply chain
            // threads correctly — see GiteaPRThreadViewModel.lastCommentId.
            if (!isOutdated) add(replyComposer(cs, thread.comments.lastOrNull()?.id ?: thread.id))
        }
    }

    /** Resolve/unresolve toggle for a review thread — mirrors the diff-editor's
     * `GiteaPRInlayComponentsFactory.createResolveRow`, just driven by [GiteaReviewThread.isResolved]
     * directly instead of a `GiteaPRThreadViewModel` (the Timeline has no [GiteaPRDiscussionsViewModels]
     * of its own). Available regardless of [isOutdated] — resolving doesn't require replying. */
    private fun resolveRow(cs: CoroutineScope, thread: GiteaReviewThread): JComponent {
        val row = HorizontalListPanel(0)
        val labelKey = if (thread.isResolved) "pull.request.action.unresolve.thread" else "pull.request.action.resolve.thread"
        val errorKey = if (thread.isResolved) "pull.request.action.unresolve.thread.error" else "pull.request.action.resolve.thread.error"
        row.add(JButton(GiteaBundle.message(labelKey)).apply {
            addActionListener {
                cs.launch {
                    try {
                        if (thread.isResolved) onUnresolveThread(thread.id) else onResolveThread(thread.id)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        NotificationGroupManager.getInstance()
                            .getNotificationGroup("Gitea")
                            .createNotification(GiteaBundle.message(labelKey), GiteaBundle.message(errorKey), NotificationType.ERROR)
                            .notify(project)
                    }
                }
            }
        })
        return row
    }

    private fun threadCommentRow(cs: CoroutineScope, comment: GiteaReviewComment): JComponent {
        val suggestion = if (comment.path != null) comment.body?.let { GiteaSuggestionUtil.detect(it) } else null
        val displayBody = suggestion?.let { GiteaSuggestionUtil.stripSuggestion(comment.body!!) } ?: comment.body
        val (bodyComponent, actionsPanel) = commentBodyAndActions(cs, comment.id, comment.author?.login, displayBody)
        val content = withSuggestion(cs, project, comment.path, bodyComponent, displayBody.isNullOrBlank(), suggestion)
        return CodeReviewChatItemUIUtil.build(
            ComponentType.COMPACT,
            { size -> avatars.getIcon(comment.author, size) },
            content,
        ) {
            withHeader(titleTextPane(actorName(comment.author), comment.author?.htmlUrl, comment.createdAt, comment.isEdited), actionsPanel)
        }
    }

    /** [CodeReviewTimelineUIUtil.createTitleTextPane] plus a small "edited" suffix when [edited]. */
    private fun titleTextPane(name: String, url: String?, timestamp: Date?, edited: Boolean): JComponent {
        val titlePane = CodeReviewTimelineUIUtil.createTitleTextPane(name, url, timestamp ?: Date())
        if (!edited) return titlePane
        return HorizontalListPanel(4).apply {
            add(titlePane)
            add(JBLabel(GiteaBundle.message("pull.request.timeline.comment.edited")).apply {
                foreground = UIUtil.getContextHelpForeground()
                font = JBFont.small()
            })
        }
    }

    /**
     * A "Reply" link that swaps in a comment composer (same [GiteaPRCommentFieldFactory] machinery
     * the top-level "leave a comment" field uses) when clicked, and swaps back once submitted.
     * Stays hidden while [currentUser] hasn't resolved yet.
     */
    private fun replyComposer(cs: CoroutineScope, threadId: Long): JComponent {
        val wrapper = Wrapper()
        cs.launch {
            currentUser.collect { user ->
                wrapper.setContent(user?.let { replyLink(cs, wrapper, threadId, it) })
                wrapper.revalidate()
                wrapper.repaint()
            }
        }
        return wrapper
    }

    private fun replyLink(cs: CoroutineScope, wrapper: Wrapper, threadId: Long, user: GiteaUser): JComponent =
        ActionLink(GiteaBundle.message("pull.request.action.reply")) {
            val replyVm = GiteaPRSubmittableTextViewModel(project, cs) { body ->
                onReplyToThread(threadId, body)
                wrapper.setContent(replyLink(cs, wrapper, threadId, user))
                wrapper.revalidate()
                wrapper.repaint()
            }
            wrapper.setContent(GiteaPRCommentFieldFactory.create(cs, replyVm, avatars, user, mentionCandidates))
            wrapper.revalidate()
            wrapper.repaint()
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
        // directly — it's @ApiStatus.Internal). Coerced to 0: a hunk with DIFF_CONTEXT_SIZE lines
        // or fewer (e.g. a minimal-context hunk near the start of a file) would otherwise compute
        // a negative truncation index.
        val truncatedHunk = PatchHunkUtil.truncateHunkBefore(hunk, (hunk.lines.lastIndex - DIFF_CONTEXT_SIZE).coerceAtLeast(0))
        val anchorRange = LineRange(truncatedHunk.lines.lastIndex, truncatedHunk.lines.size)

        val diffComponent = TimelineDiffComponentFactory.createDiffComponentIn(
            cs, project, EditorFactory.getInstance(), truncatedHunk, anchorRange,
        )
        return TimelineDiffComponentFactory.createDiffWithHeader(cs, path, flowOf(null), diffComponent)
    }

    /** The verdict text — Approved/Request Changes/Comment. Plain text, no icon and no accent
     * bar of its own: the whole review's border ([review]) is already tinted to the same verdict
     * color via [reviewAccentColor], so a second, redundant color indicator here isn't needed. */
    private fun reviewStateLabel(state: GiteaReviewState): JComponent {
        val key = when (state) {
            GiteaReviewState.APPROVED -> "pull.request.timeline.review.approved"
            GiteaReviewState.REQUEST_CHANGES -> "pull.request.timeline.review.changes"
            else -> "pull.request.timeline.review.commented"
        }
        return JBLabel(GiteaBundle.message(key)).apply { font = JBFont.label().asBold() }
    }

    private fun reviewStatusType(state: GiteaReviewState): StatusMessageType = when (state) {
        GiteaReviewState.APPROVED -> StatusMessageType.SUCCESS
        GiteaReviewState.REQUEST_CHANGES -> StatusMessageType.WARNING
        else -> StatusMessageType.INFO
    }

    /** The exact named colors [StatusMessageComponentFactory] paints its accent bar with, reused
     * here so a review's border matches its verdict's bar instead of an arbitrary neutral gray —
     * same named-color keys the platform itself registers, so light/dark theming stays automatic. */
    private fun reviewAccentColor(type: StatusMessageType): JBColor = when (type) {
        StatusMessageType.SUCCESS -> JBColor.namedColor("Review.MetaInfo.StatusLine.Green", ColorUtil.fromHex("62B543B3"))
        StatusMessageType.WARNING, StatusMessageType.ERROR ->
            JBColor.namedColor("Review.MetaInfo.StatusLine.Orange", ColorUtil.fromHex("F26522B3"))
        StatusMessageType.INFO -> JBColor.namedColor("Review.MetaInfo.StatusLine.Blue", ColorUtil.fromHex("40B6E0B2"))
        StatusMessageType.SECONDARY_INFO -> JBColor.namedColor("Review.MetaInfo.StatusLine.Gray", ColorUtil.fromHex("9AA7B0B3"))
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
        GiteaTimelineItem.Event.Kind.TITLE_CHANGED -> GiteaBundle.message("pull.request.timeline.event.title.changed", item.oldValue ?: "n/a", item.newValue ?: "n/a")
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
            GiteaBundle.message("pull.request.timeline.event.referenced.from.commit", item.newValue?.take(7) ?: "")

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

    private fun actorName(user: GiteaUser?, rawUser: String?): String = user?.let { it.fullName ?: it.login } ?: rawUser.orEmpty()
    private fun actorName(user: GiteaUser?): String = user?.let { it.fullName ?: it.login }.orEmpty()

    private fun bodyHtml(body: String?): String =
        if (body.isNullOrBlank()) "<i>${esc(GiteaBundle.message("pull.request.timeline.no.body"))}</i>"
        else esc(body).replace("\n", "<br>")

    private fun esc(s: String): String = StringUtil.escapeXmlEntities(s)

    private companion object {
        const val DIFF_CONTEXT_SIZE = 3
    }
}
