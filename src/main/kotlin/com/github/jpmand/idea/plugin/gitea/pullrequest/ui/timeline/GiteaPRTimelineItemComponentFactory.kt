package com.github.jpmand.idea.plugin.gitea.pullrequest.ui.timeline

import com.github.jpmand.idea.plugin.gitea.api.models.*
import com.github.jpmand.idea.plugin.gitea.pullrequest.review.GiteaSuggestionUtil
import com.github.jpmand.idea.plugin.gitea.pullrequest.ui.comment.GiteaPRCommentFieldFactory
import com.github.jpmand.idea.plugin.gitea.pullrequest.ui.comment.GiteaPRSubmittableTextViewModel
import com.github.jpmand.idea.plugin.gitea.pullrequest.ui.commentBodyPane
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
import com.intellij.ide.BrowserUtil
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.diff.impl.patch.PatchHunkUtil
import com.intellij.openapi.diff.impl.patch.PatchReader
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.HtmlBuilder
import com.intellij.openapi.util.text.HtmlChunk
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import java.awt.Dimension
import java.awt.datatransfer.StringSelection
import java.util.*
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.border.EmptyBorder

/**
 * Renders one [GiteaPRTimelineItemViewModel] using the platform timeline-item shell
 * ([CodeReviewChatItemUIUtil.build] / [StatusMessageComponentFactory] /
 * [TimelineThreadCommentsPanel]) — the same building blocks the bundled GitLab/GitHub timeline
 * factories use — with its own header pane (see [titleTextPane]).
 */
@Suppress("UnstableApiUsage")
class GiteaPRTimelineItemComponentFactory(
    private val project: Project,
    private val avatars: IconsProvider<GiteaUser>,
    /** Renders a body to sanitized HTML via the server; null on failure (keep the fallback). */
    private val renderMarkdown: suspend (String) -> String?,
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
        is GiteaPRTimelineItemViewModel.Event -> event(cs, item)
    }

    // ── item kinds ─────────────────────────────────────────────────────────

    private fun comment(cs: CoroutineScope, item: GiteaPRTimelineItemViewModel.Comment): JComponent {
        val (pane, actionsPanel) = commentBodyAndActions(cs, item.id, item.actor?.login, item.body)
        return chatItem(item, pane, commentUrlActions(item.htmlUrl), actionsPanel, edited = item.edited, action = HtmlChunk.text(GiteaBundle.message("pull.request.timeline.commented")))
    }

    /**
     * A review as one timeline item: "Carol requested changes · 2 minutes ago" as its header, then
     * a frame in the verdict's colour around the review body and each of its threads, so the whole
     * review reads as one unit. A review with neither gets no frame, just the header.
     */
    private fun review(cs: CoroutineScope, item: GiteaPRTimelineItemViewModel.Review): JComponent {
        val threads = item.threads.mapNotNull { thread -> threadItem(cs, thread) }
        val body = item.body?.takeIf { it.isNotBlank() }?.let { commentBodyPane(cs, it, renderMarkdown) }
        val actions = urlActions(item.htmlUrl, "pull.request.action.view.review.in.browser", "pull.request.action.copy.review.url")
        body?.let { installMenu(it, actions) }
        val content = if (body == null && threads.isEmpty()) {
            emptyContent()
        } else {
            VerticalListPanel(CodeReviewTimelineUIUtil.VERTICAL_GAP).apply {
                val accent = reviewAccentColor(reviewStatusType(item.state))
                border = JBUI.Borders.compound(RoundedLineBorder(accent, JBUI.scale(REVIEW_FRAME_ARC), 1), JBUI.Borders.empty(8, 0))
                if (body != null) add(Wrapper(body).apply { border = JBUI.Borders.empty(0, 8) })
                threads.forEach { add(it) }
            }
        }
        return CodeReviewChatItemUIUtil.build(ComponentType.FULL, { size -> avatars.getIcon(item.actor, size) }, content) {
            maxContentWidth = null
            withHeader(
                titleTextPane(actorName(item.actor), item.actor?.htmlUrl, item.timestamp, false,
                    action = HtmlChunk.text(GiteaBundle.message(reviewStateKey(item.state))), menu = actions),
                null,
            )
        }
    }

    /** The platform's status-line colour for [type], so the review's frame shows its verdict. */
    private fun reviewAccentColor(type: StatusMessageType): JBColor = when (type) {
        StatusMessageType.SUCCESS -> JBColor.namedColor("Review.MetaInfo.StatusLine.Green", ColorUtil.fromHex("62B543B3"))
        StatusMessageType.WARNING, StatusMessageType.ERROR ->
            JBColor.namedColor("Review.MetaInfo.StatusLine.Orange", ColorUtil.fromHex("F26522B3"))
        StatusMessageType.INFO -> JBColor.namedColor("Review.MetaInfo.StatusLine.Blue", ColorUtil.fromHex("40B6E0B2"))
        StatusMessageType.SECONDARY_INFO -> JBColor.namedColor("Review.MetaInfo.StatusLine.Gray", ColorUtil.fromHex("9AA7B0B3"))
    }

    private fun commits(item: GiteaPRTimelineItemViewModel.Commits): JComponent {
        // One status line around the whole list, as in the GitHub plugin, not one per commit.
        val list = VerticalListPanel(4).apply {
            item.commits.forEach { c -> add(commitRow(c)) }
        }
        val action = GiteaBundle.message(
            if (item.commits.size == 1) "pull.request.timeline.commit.added.one"
            else "pull.request.timeline.commit.added.many",
            item.commits.size,
        )
        return CodeReviewChatItemUIUtil.build(
            ComponentType.FULL,
            { size -> avatars.getIcon(item.actor, size) },
            StatusMessageComponentFactory.create(list, StatusMessageType.INFO),
        ) {
            withHeader(
                titleTextPane(actorName(item.actor, item.rawActor), item.actor?.htmlUrl, item.timestamp, false, action = HtmlChunk.text(action)),
                null,
            )
        }
    }

    /** One commit: hash (linked, opens the commit) + message title (plain text, not a link) on
     * the first row, committer + commit time (formatted like the item headers) on a second. */
    private fun commitRow(c: GiteaTimelineItem.Commit): JComponent {
        val titleRow = HorizontalListPanel(6).apply {
            add(ActionLink(c.shortSha) { onOpenCommit(c.sha) })
            add(JBLabel(c.messageTitle))
        }
        val metaRow = JBLabel("${actorName(c.actor, c.rawAuthor)} ${DateFormatUtil.formatPrettyDateTime(c.timestamp)}").apply {
            foreground = UIUtil.getContextHelpForeground()
            font = JBFont.small()
        }
        return VerticalListPanel(0).apply { add(titleRow); add(metaRow) }
    }

    /**
     * An activity event as a one-line timeline item: the actor's avatar, then "Bob requested a
     * review from alice · 2 minutes ago". A commit it references links to that commit in the IDE.
     */
    private fun event(cs: CoroutineScope, item: GiteaPRTimelineItemViewModel.Event): JComponent {
        val sha = item.newValue
        val action = if (item.kind == GiteaTimelineItem.Event.Kind.REFERENCED_FROM_COMMIT && sha != null) {
            HtmlChunk.fragment(
                HtmlChunk.text(GiteaBundle.message("pull.request.timeline.event.referenced.from.commit", "").trimEnd()),
                HtmlChunk.nbsp(),
                HtmlChunk.link(COMMIT_LINK_PREFIX + sha, sha.take(7)),
            )
        } else {
            HtmlChunk.text(eventText(item))
        }
        return chatItem(item, emptyContent(), emptyList(), action = action)
    }

    /** Stands in for an item's content when everything it has to say fits in its header. */
    private fun emptyContent(): JComponent = JPanel(null).apply {
        isOpaque = false
        preferredSize = Dimension(0, 0)
    }

    // ── shell + helpers ────────────────────────────────────────────────────

    private fun chatItem(
        item: GiteaPRTimelineItemViewModel,
        content: JComponent,
        actions: List<AnAction>,
        actionsPanel: JComponent? = null,
        edited: Boolean = false,
        /** What the actor did, shown between their name and the date ("commented"). */
        action: HtmlChunk? = null,
    ): JComponent {
        installMenu(content, actions)
        return CodeReviewChatItemUIUtil.build(
            ComponentType.FULL,
            { size -> avatars.getIcon(item.actor, size) },
            content,
        ) {
            withHeader(
                titleTextPane(actorName(item.actor), item.actor?.htmlUrl, item.timestamp, edited, action, menu = actions),
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
        val pane = commentBodyPane(cs, body, renderMarkdown)
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
     * A review thread as its own timeline item, as in the GitHub plugin: the first comment is a
     * full-width item (the diff isn't squeezed to text width) showing the diff hunk around the
     * anchored line (from its
     * [com.github.jpmand.idea.plugin.gitea.api.models.GiteaReviewComment.diffHunk]) above its text,
     * with Outdated/Resolved tags next to the author; replies follow with smaller avatars, lined up
     * under the first comment's text, and Resolve/Reply share one row below them.
     */
    private fun threadItem(cs: CoroutineScope, thread: GiteaReviewThread): JComponent? {
        val first = thread.comments.firstOrNull() ?: return null
        val suggestion = if (first.path != null) first.body?.let { GiteaSuggestionUtil.detect(it) } else null
        val displayBody = suggestion?.let { GiteaSuggestionUtil.stripSuggestion(first.body!!) } ?: first.body
        val (bodyComponent, actionsPanel) = commentBodyAndActions(cs, first.id, first.author?.login, displayBody)
        val firstActions = commentUrlActions(first.htmlUrl)
        installMenu(bodyComponent, firstActions)
        val firstContent = VerticalListPanel(THREAD_DIFF_TEXT_GAP).apply {
            diffHunkComponent(cs, thread.path, first.diffHunk)?.let { add(it) }
            add(withSuggestion(cs, project, first.path, bodyComponent, displayBody.isNullOrBlank(), suggestion))
        }
        val tags = listOfNotNull(
            CollaborationToolsUIUtil.createTagLabel(CollaborationToolsBundle.message("review.thread.outdated.tag")).takeIf { thread.isOutdated },
            CollaborationToolsUIUtil.createTagLabel(CollaborationToolsBundle.message("review.thread.resolved.tag")).takeIf { thread.isResolved },
        )
        val firstItem = CodeReviewChatItemUIUtil.build(
            ComponentType.FULL,
            { size -> avatars.getIcon(first.author, size) },
            firstContent,
        ) {
            maxContentWidth = null
            val title = titleTextPane(actorName(first.author), first.author?.htmlUrl, first.createdAt, first.isEdited, menu = firstActions)
            val header = if (tags.isEmpty()) title else HorizontalListPanel(CodeReviewCommentUIUtil.Title.HORIZONTAL_GAP).apply {
                add(title)
                tags.forEach(::add)
            }
            withHeader(header, actionsPanel)
        }

        val replies = thread.comments.drop(1)
        val replyComposer = Wrapper()
        val actionsRow = HorizontalListPanel(THREAD_ACTIONS_GAP).apply {
            border = EmptyBorder(JBUI.scale(2), ComponentType.FULL.fullLeftShift, JBUI.scale(6), 0)
            add(resolveLink(cs, thread))
            // Outdated threads (anchored to a diff that's no longer current) can't be replied to.
            // Reply targets the thread's last comment (not the anchor) so Gitea's reply chain
            // threads correctly — see GiteaPRThreadViewModel.lastCommentId.
            if (!thread.isOutdated) add(replyLink(cs, thread.comments.last().id, replyComposer))
        }
        return VerticalListPanel(0).apply {
            add(firstItem)
            if (replies.isNotEmpty()) add(createThreadCommentsPanel(replies) { c -> replyItem(cs, c) })
            add(actionsRow)
            add(replyComposer)
        }
    }

    /** Resolve/unresolve toggle for a review thread, with the platform's wording — mirrors the
     * diff-editor inlays' one, just driven by [GiteaReviewThread.isResolved] directly (the Timeline
     * has no discussions view model of its own). Available regardless of whether the thread is
     * outdated — resolving doesn't require replying. */
    private fun resolveLink(cs: CoroutineScope, thread: GiteaReviewThread): JComponent {
        val labelKey = if (thread.isResolved) "pull.request.action.unresolve.thread" else "pull.request.action.resolve.thread"
        val errorKey = if (thread.isResolved) "pull.request.action.unresolve.thread.error" else "pull.request.action.resolve.thread.error"
        return ActionLink(CodeReviewCommentUIUtil.getResolveToggleActionText(thread.isResolved)) {
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
    }

    /** A reply in a thread: a smaller avatar, lined up under the thread's first comment text. */
    private fun replyItem(cs: CoroutineScope, comment: GiteaReviewComment): JComponent {
        val suggestion = if (comment.path != null) comment.body?.let { GiteaSuggestionUtil.detect(it) } else null
        val displayBody = suggestion?.let { GiteaSuggestionUtil.stripSuggestion(comment.body!!) } ?: comment.body
        val (bodyComponent, actionsPanel) = commentBodyAndActions(cs, comment.id, comment.author?.login, displayBody)
        val actions = commentUrlActions(comment.htmlUrl)
        installMenu(bodyComponent, actions)
        val content = withSuggestion(cs, project, comment.path, bodyComponent, displayBody.isNullOrBlank(), suggestion)
        return CodeReviewChatItemUIUtil.build(
            ComponentType.FULL_SECONDARY,
            { size -> avatars.getIcon(comment.author, size) },
            content,
        ) {
            withHeader(titleTextPane(actorName(comment.author), comment.author?.htmlUrl, comment.createdAt, comment.isEdited, menu = actions), actionsPanel)
        }
    }

    /**
     * An item's header: the author in bold (linked to their profile), what they did, and when —
     * "Bob commented 2 minutes ago" — plus a small "edited" suffix when [edited]. Built like the platform's
     * [CodeReviewTimelineUIUtil.createTitleTextPane], which has no room for the [action] text.
     * Links in [action] open in the browser, except [COMMIT_LINK_PREFIX] ones, which open the
     * commit in the IDE. [menu] is its right-click menu.
     */
    private fun titleTextPane(
        name: String, url: String?, timestamp: Date?, edited: Boolean,
        action: HtmlChunk? = null, menu: List<AnAction> = emptyList(),
    ): JComponent {
        val author = (if (url != null) HtmlChunk.link(url, name) else HtmlChunk.text(name))
            .wrapWith(HtmlChunk.span().setClass("author-name")).bold()
        val html = HtmlBuilder().append(author)
        if (action != null) html.append(HtmlChunk.nbsp()).append(action)
        html.append(HtmlChunk.nbsp()).append(DateFormatUtil.formatPrettyDateTime(timestamp ?: Date()))
        val titlePane = SimpleHtmlPane(addBrowserListener = false).apply {
            setHtmlBody(html.toString())
            onHyperlinkActivated { e ->
                val href = e.description.orEmpty()
                if (href.startsWith(COMMIT_LINK_PREFIX)) onOpenCommit(href.removePrefix(COMMIT_LINK_PREFIX))
                else BrowserUtil.browse(href)
            }
        }
        installMenu(titlePane, menu)
        if (!edited) return titlePane
        return HorizontalListPanel(CodeReviewCommentUIUtil.Title.HORIZONTAL_GAP).apply {
            add(titlePane)
            add(JBLabel(GiteaBundle.message("pull.request.timeline.comment.edited")).apply {
                foreground = UIUtil.getContextHelpForeground()
                font = JBFont.small()
            })
        }
    }

    /**
     * A "Reply" link that shows a comment composer (same [GiteaPRCommentFieldFactory] machinery the
     * top-level "leave a comment" field uses) in [composer], lined up with the replies, and hides it
     * again once submitted or cancelled. Stays hidden while [currentUser] hasn't resolved yet.
     */
    private fun replyLink(cs: CoroutineScope, threadId: Long, composer: Wrapper): JComponent {
        lateinit var link: ActionLink
        var composerOpen = false
        fun close() {
            composerOpen = false
            composer.setContent(null)
            link.isVisible = true
            composer.revalidate()
            composer.repaint()
        }
        link = ActionLink(CollaborationToolsBundle.message("review.comments.reply.action")) {
            val user = currentUser.value ?: return@ActionLink
            val replyVm = GiteaPRSubmittableTextViewModel(project, cs) { body ->
                onReplyToThread(threadId, body)
                close()
            }
            composerOpen = true
            link.isVisible = false
            composer.setContent(
                GiteaPRCommentFieldFactory.create(
                    cs, replyVm, avatars, user, mentionCandidates,
                    onCancel = ::close, componentType = ComponentType.FULL_SECONDARY, isReply = true,
                ),
            )
            composer.revalidate()
            composer.repaint()
        }
        cs.launch { currentUser.collect { link.isVisible = it != null && !composerOpen } }
        return link
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

    private fun reviewStateKey(state: GiteaReviewState): String = when (state) {
        GiteaReviewState.APPROVED -> "pull.request.timeline.review.approved"
        GiteaReviewState.REQUEST_CHANGES -> "pull.request.timeline.review.changes"
        GiteaReviewState.PENDING -> "pull.request.timeline.review.pending"
        GiteaReviewState.REQUEST_REVIEW -> "pull.request.timeline.review.requested"
        GiteaReviewState.COMMENT -> "pull.request.timeline.review.commented"
    }

    /** The verdict's status-line colour, as the GitHub plugin maps review states. */
    private fun reviewStatusType(state: GiteaReviewState): StatusMessageType = when (state) {
        GiteaReviewState.APPROVED -> StatusMessageType.SUCCESS
        GiteaReviewState.REQUEST_CHANGES -> StatusMessageType.ERROR
        GiteaReviewState.PENDING -> StatusMessageType.SECONDARY_INFO
        GiteaReviewState.COMMENT, GiteaReviewState.REQUEST_REVIEW -> StatusMessageType.INFO
    }

    /** "View Comment in Browser" / "Copy Comment URL" for a comment's web page. */
    private fun commentUrlActions(url: String?): List<AnAction> =
        urlActions(url, "pull.request.action.view.comment.in.browser", "pull.request.action.copy.comment.url")

    private fun installMenu(component: JComponent, actions: List<AnAction>) {
        if (actions.isNotEmpty()) PopupHandler.installPopupMenu(component, DefaultActionGroup(actions), "GiteaPRTimelinePopup")
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

    private fun esc(s: String): String = StringUtil.escapeXmlEntities(s)

    private companion object {
        const val DIFF_CONTEXT_SIZE = 3
        /** Between a thread's diff and its first comment's text (the platform's thread diff gap). */
        const val THREAD_DIFF_TEXT_GAP = 8
        /** Between a thread's Resolve and Reply links, as in the GitHub plugin. */
        const val THREAD_ACTIONS_GAP = 14
        /** Corner rounding of a review's coloured frame. */
        const val REVIEW_FRAME_ARC = 12
        /** Marks a header link that opens a commit in the IDE rather than in the browser. */
        const val COMMIT_LINK_PREFIX = "gitea-commit:"
    }
}
