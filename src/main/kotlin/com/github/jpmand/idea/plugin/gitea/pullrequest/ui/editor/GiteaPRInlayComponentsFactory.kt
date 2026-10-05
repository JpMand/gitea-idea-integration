package com.github.jpmand.idea.plugin.gitea.pullrequest.ui.editor

import com.github.jpmand.idea.plugin.gitea.api.models.GiteaPRDraftComment
import com.github.jpmand.idea.plugin.gitea.api.models.GiteaUser
import com.github.jpmand.idea.plugin.gitea.pullrequest.review.GiteaPRCommentViewModel
import com.github.jpmand.idea.plugin.gitea.pullrequest.review.GiteaPRDiscussionsViewModels
import com.github.jpmand.idea.plugin.gitea.pullrequest.review.GiteaPRThreadViewModel
import com.github.jpmand.idea.plugin.gitea.pullrequest.review.GiteaSuggestionUtil
import com.github.jpmand.idea.plugin.gitea.pullrequest.ui.comment.GiteaPRCommentFieldFactory
import com.github.jpmand.idea.plugin.gitea.pullrequest.ui.comment.GiteaPRSubmittableTextViewModel
import com.github.jpmand.idea.plugin.gitea.pullrequest.ui.commentBodyPane
import com.github.jpmand.idea.plugin.gitea.pullrequest.ui.createSuggestionDiffBox
import com.github.jpmand.idea.plugin.gitea.pullrequest.ui.createThreadCommentsPanel
import com.github.jpmand.idea.plugin.gitea.pullrequest.ui.withSuggestion
import com.github.jpmand.idea.plugin.gitea.util.GiteaBundle
import com.intellij.collaboration.async.mapState
import com.intellij.collaboration.messages.CollaborationToolsBundle
import com.intellij.collaboration.ui.CollaborationToolsUIUtil
import com.intellij.collaboration.ui.EditableComponentFactory
import com.intellij.collaboration.ui.HorizontalListPanel
import com.intellij.collaboration.ui.SimpleHtmlPane
import com.intellij.collaboration.ui.VerticalListPanel
import com.intellij.collaboration.ui.codereview.CodeReviewChatItemUIUtil
import com.intellij.collaboration.ui.codereview.CodeReviewChatItemUIUtil.ComponentType
import com.intellij.collaboration.ui.codereview.CodeReviewTimelineUIUtil
import com.intellij.collaboration.ui.codereview.comment.CodeReviewCommentUIUtil
import com.intellij.collaboration.ui.codereview.comment.CodeReviewSubmittableTextViewModelBase
import com.intellij.collaboration.ui.codereview.comment.CodeReviewTextEditingViewModel
import com.intellij.collaboration.ui.codereview.editor.CodeReviewComponentInlayRenderer
import com.intellij.diff.util.DiffDrawUtil
import com.intellij.diff.util.TextDiffType
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.ComponentInlayRenderer
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.HtmlChunk
import com.intellij.ui.JBColor
import com.intellij.ui.components.ActionLink
import com.intellij.ui.components.panels.Wrapper
import com.intellij.ui.hover.HoverStateListener
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.awt.Color
import java.awt.Component
import java.util.Date
import javax.swing.*
import javax.swing.border.EmptyBorder

private val LOG = logger<GiteaPRInlayComponentsFactory>()

/** Existing comment threads (read-only display plus resolve/unresolve/reply/edit/delete) and
 * new-comment composer inlays (a line comment, drafted locally until the whole review is
 * submitted — see [GiteaPRNewCommentEditorViewModel]).
 *
 * Comment rows are built with [CodeReviewChatItemUIUtil.build] (avatar + header + hover-reveal
 * actions) — the same platform shell
 * [com.github.jpmand.idea.plugin.gitea.pullrequest.ui.timeline.GiteaPRTimelineItemComponentFactory]
 * uses for the Timeline's own comment rows, so a thread looks the same whether it's read from the
 * diff editor or the Timeline. */
@Suppress("UnstableApiUsage")
object GiteaPRInlayComponentsFactory {

    /** Between a thread's Resolve and Reply links, as in the GitHub plugin's inlays. */
    private const val THREAD_ACTIONS_GAP = 14
    private const val INLAY_VERTICAL_MARGIN = 4

    fun createRenderer(
        project: Project,
        cs: CoroutineScope,
        model: GiteaPRInlayModel,
        discussionsVm: GiteaPRDiscussionsViewModels,
    ): ComponentInlayRenderer<JComponent> =
        when (model) {
            is GiteaPRInlayModel.Thread -> {
                val card = CodeReviewCommentUIUtil.createEditorInlayPanel(withInlayPadding(createThreadPanel(project, cs, model.vm, discussionsVm)))
                installHoverAnchorHighlight(card, model.editor, model.editorLineIdx)
                CodeReviewComponentInlayRenderer(withInlayMargin(card))
            }
            is GiteaPRInlayModel.NewComment -> CodeReviewComponentInlayRenderer(
                withInlayMargin(CodeReviewCommentUIUtil.createEditorInlayPanel(withInlayPadding(createNewCommentPanel(project, cs, model.vm, discussionsVm)))),
            )
        }

    /** A little room around each card, so two inlays on one line don't touch (the inlay machinery adds none). */
    private fun withInlayMargin(card: JComponent): JComponent =
        Wrapper(card).apply { border = JBUI.Borders.empty(INLAY_VERTICAL_MARGIN, 0) }

    /** The platform's padding inside a compact inlay card (as the GitHub plugin's inlays have), so
     * the first and last rows don't touch the rounded border. */
    private fun withInlayPadding(content: JComponent): JComponent =
        content.apply { border = EmptyBorder(CodeReviewCommentUIUtil.getInlayPadding(ComponentType.COMPACT)) }

    /**
     * While the card is hovered, highlights [lineIdx] in [editor] the same way
     * [com.intellij.collaboration.ui.codereview.timeline.TimelineDiffComponentFactory]'s own
     * diff-hunk preview highlights its anchor line (`AnchorLine`, same named color) — via
     * [DiffDrawUtil.createHighlighter], the public/stable diff API, disposing the highlighter on
     * hover-out.
     */
    private fun installHoverAnchorHighlight(card: JComponent, editor: Editor, lineIdx: Int) {
        object : HoverStateListener() {
            private var highlighters: List<RangeHighlighter> = emptyList()

            override fun hoverChanged(component: Component, hovered: Boolean) {
                highlighters.forEach { it.dispose() }
                highlighters = if (hovered) {
                    DiffDrawUtil.createHighlighter(editor, lineIdx, lineIdx + 1, CommentAnchorLineType, false)
                } else {
                    emptyList()
                }
            }
        }.apply { mouseExited(card) }.addTo(card)
    }

    /** Mirrors [com.intellij.collaboration.ui.codereview.timeline.TimelineDiffComponentFactory]'s
     * internal `AnchorLine` (same named color, same fallback) — that one is `@ApiStatus.Internal`
     * only because it lives in `collaboration-tools`; [TextDiffType] itself is public/stable. */
    private object CommentAnchorLineType : TextDiffType {
        override fun getName(): String = "Gitea Comment Anchor Line"
        override fun getColor(editor: Editor?): Color =
            JBColor.namedColor("Review.Timeline.Thread.Diff.AnchorLine", JBColor(0xFBF1D1, 0x544B2D))
        override fun getIgnoredColor(editor: Editor?): Color = getColor(editor)
        override fun getMarkerColor(editor: Editor?): Color = getColor(editor)
    }

    private fun createThreadPanel(
        project: Project,
        cs: CoroutineScope,
        vm: GiteaPRThreadViewModel,
        discussionsVm: GiteaPRDiscussionsViewModels,
    ): JComponent {
        val firstComment = vm.commentVMs.firstOrNull()
        val commentsPanel = createThreadCommentsPanel(vm.commentVMs) { commentVm ->
            createCommentPanel(project, cs, discussionsVm, commentVm, if (commentVm === firstComment) threadTags(vm) else emptyList())
        }

        // Read without an account: the thread is shown, but can't be resolved or replied to.
        if (discussionsVm.isAnonymous) return commentsPanel

        // Resolve and Reply share one row, lined up with the comment text (as in the GitHub
        // plugin's inlays); Reply swaps in a composer below the row.
        val replyComposer = Wrapper()
        val actionsRow = HorizontalListPanel(THREAD_ACTIONS_GAP).apply {
            border = EmptyBorder(JBUI.scale(4), ComponentType.COMPACT.fullLeftShift, JBUI.scale(2), 0)
            add(createResolveLink(project, cs, vm))
            // Outdated threads (anchored to a diff that's no longer current) can't be replied to.
            if (!vm.isOutdated) add(replyLink(project, cs, discussionsVm, vm.lastCommentId, replyComposer))
        }

        return VerticalListPanel(0).apply {
            add(commentsPanel)
            add(actionsRow)
            add(replyComposer)
        }
    }

    /** Tags shown next to a thread's first comment: Resolved and/or Outdated, with the platform's wording. */
    private fun threadTags(vm: GiteaPRThreadViewModel): List<JComponent> = listOfNotNull(
        CollaborationToolsUIUtil.createTagLabel(CollaborationToolsBundle.message("review.thread.resolved.tag")).takeIf { vm.isResolved },
        CollaborationToolsUIUtil.createTagLabel(CollaborationToolsBundle.message("review.thread.outdated.tag")).takeIf { vm.isOutdated },
    )

    /**
     * A "Reply" link that shows a comment composer (same [GiteaPRCommentFieldFactory] machinery the
     * Timeline's thread-reply/leave-a-comment fields use) in [composer], and hides it again once
     * submitted or cancelled. Stays hidden while [GiteaPRDiscussionsViewModels.currentUser] hasn't
     * resolved yet.
     */
    private fun replyLink(
        project: Project,
        cs: CoroutineScope,
        discussionsVm: GiteaPRDiscussionsViewModels,
        threadId: Long,
        composer: Wrapper,
    ): JComponent {
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
            val user = discussionsVm.currentUser.value ?: return@ActionLink
            val replyVm = GiteaPRSubmittableTextViewModel(project, cs) { body ->
                discussionsVm.replyToThread(threadId, body)
                close()
            }
            composerOpen = true
            link.isVisible = false
            composer.setContent(
                GiteaPRCommentFieldFactory.create(
                    cs, replyVm, discussionsVm.avatars, user, discussionsVm.mentionCandidates,
                    onCancel = ::close, componentType = ComponentType.COMPACT, isReply = true,
                ),
            )
            composer.revalidate()
            composer.repaint()
        }
        cs.launch { discussionsVm.currentUser.collect { link.isVisible = it != null && !composerOpen } }
        return link
    }

    /**
     * A new-comment inlay: shows the composer ([GiteaPRCommentFieldFactory], with a Cancel action
     * this time) while [GiteaPRNewCommentEditorViewModel.draft] is `null`, then switches to a
     * compact, locally editable/removable draft row once submitted — never a network call itself,
     * see [GiteaPRNewCommentEditorViewModel].
     */
    private fun createNewCommentPanel(
        project: Project,
        cs: CoroutineScope,
        vm: GiteaPRNewCommentEditorViewModel,
        discussionsVm: GiteaPRDiscussionsViewModels,
    ): JComponent {
        val wrapper = Wrapper()
        cs.launch {
            combine(vm.draft, discussionsVm.currentUser) { draft, user -> draft to user }.collect { (draft, user) ->
                wrapper.setContent(
                    when {
                        draft != null -> createDraftRow(project, cs, discussionsVm, vm, draft, user)
                        user != null -> createComposerPanel(project, cs, vm, discussionsVm, user)
                        else -> null
                    },
                )
                wrapper.revalidate()
                wrapper.repaint()
            }
        }
        return wrapper
    }

    /** The composer itself — a plain [GiteaPRCommentFieldFactory] field for an ordinary new
     * comment, or (when [GiteaPRNewCommentEditorViewModel.suggestion] is set) that same field for
     * an optional explanation with the suggestion's own read-only diff preview above it, so the
     * raw marker+fence text it submits as is never shown to the user. */
    private fun createComposerPanel(
        project: Project,
        cs: CoroutineScope,
        vm: GiteaPRNewCommentEditorViewModel,
        discussionsVm: GiteaPRDiscussionsViewModels,
        user: GiteaUser,
    ): JComponent {
        val commentField = GiteaPRCommentFieldFactory.create(
            cs, vm.textVm, discussionsVm.avatars, user, discussionsVm.mentionCandidates, onCancel = vm::cancel,
            primaryActionLabelKey = vm.reviewInProgress.mapState { inProgress ->
                if (inProgress) "pull.request.action.add.review.comment" else "pull.request.action.start.review"
            },
            secondaryAction = vm.reviewInProgress.mapState { inProgress ->
                if (inProgress) null
                else GiteaPRCommentFieldFactory.SecondaryAction("pull.request.action.send.single.comment.review", vm::submitAsSingleCommentReview)
            },
            componentType = ComponentType.COMPACT,
        )
        val suggestion = vm.suggestion ?: return commentField
        val padding = ComponentType.COMPACT.inputPaddingInsets
        return VerticalListPanel(0).apply {
            // Lined up with the field below, which carries the same input padding.
            add(createSuggestionDiffBox(cs, project, suggestion).let {
                Wrapper(it).apply { border = EmptyBorder(padding.top, padding.left, 0, padding.right) }
            })
            add(commentField)
        }
    }

    private fun createDraftRow(
        project: Project,
        cs: CoroutineScope,
        discussionsVm: GiteaPRDiscussionsViewModels,
        vm: GiteaPRNewCommentEditorViewModel,
        draft: GiteaPRDraftComment,
        user: GiteaUser?,
    ): JComponent {
        // Same detect-and-strip as an already-posted comment (see createCommentPanel) — the
        // finalized draft's body carries the encoded suggestion block the same way a real comment
        // would, so it gets the same rendered-diff treatment instead of showing raw marker+fence
        // text. No Apply button here though: this composer only exists because a local edit made
        // it, so the suggested change is already applied to the working copy by definition.
        val suggestion = GiteaSuggestionUtil.detect(draft.body)
        val displayBody = suggestion?.let { GiteaSuggestionUtil.stripSuggestion(draft.body) } ?: draft.body
        val editVmFlow = MutableStateFlow<CodeReviewTextEditingViewModel?>(null)
        val textComponent = EditableComponentFactory.wrapTextComponent(cs, commentBodyPane(cs, displayBody), editVmFlow)
        val bodyComponent = if (suggestion == null) {
            textComponent
        } else {
            VerticalListPanel(4).apply {
                if (displayBody.isNotBlank()) add(textComponent)
                add(createSuggestionDiffBox(cs, project, suggestion))
            }
        }

        val actionsPanel = HorizontalListPanel(CodeReviewCommentUIUtil.Actions.HORIZONTAL_GAP).apply {
            add(CodeReviewCommentUIUtil.createEditButton {
                val editVm = DraftEditViewModel(project, cs, draft.body, draft.localId, discussionsVm, vm::updateDraftBody) { editVmFlow.value = null }
                editVmFlow.value = editVm
                editVm.requestFocus()
            })
            add(CodeReviewCommentUIUtil.createDeleteCommentIconButton { vm.removeDraft() })
        }

        return CodeReviewChatItemUIUtil.build(
            ComponentType.COMPACT,
            { size -> discussionsVm.avatars.getIcon(user, size) },
            bodyComponent,
        ) {
            val header = HorizontalListPanel(CodeReviewCommentUIUtil.Title.HORIZONTAL_GAP).apply {
                add(SimpleHtmlPane(HtmlChunk.text(authorName(user)).bold().toString()))
                add(CollaborationToolsUIUtil.createTagLabel(GiteaBundle.message("pull.request.diff.draft.badge")))
            }
            withHeader(header, actionsPanel)
        }
    }

    private class DraftEditViewModel(
        project: Project,
        cs: CoroutineScope,
        initialText: String,
        private val localId: Long,
        private val discussionsVm: GiteaPRDiscussionsViewModels,
        /** [GiteaPRNewCommentEditorViewModel.updateDraftBody] — the composing inlay's own
         * [GiteaPRNewCommentEditorViewModel.draft] is what [createDraftRow] actually renders from,
         * and it's seeded once from [discussionsVm] but never re-reads it afterward, so an edit
         * needs to update both or the row keeps showing the pre-edit body. */
        private val onBodyUpdated: (String) -> Unit,
        private val onDone: () -> Unit,
    ) : CodeReviewSubmittableTextViewModelBase(project, cs, initialText), CodeReviewTextEditingViewModel {
        override fun save() {
            submit { newBody ->
                discussionsVm.updateDraft(localId, newBody)
                onBodyUpdated(newBody)
                onDone()
            }
        }

        override fun stopEditing() = onDone()
    }

    private fun createResolveLink(project: Project, cs: CoroutineScope, vm: GiteaPRThreadViewModel): JComponent {
        val labelKey = if (vm.isResolved) "pull.request.action.unresolve.thread" else "pull.request.action.resolve.thread"
        val errorKey = if (vm.isResolved) "pull.request.action.unresolve.thread.error" else "pull.request.action.resolve.thread.error"
        return ActionLink(CodeReviewCommentUIUtil.getResolveToggleActionText(vm.isResolved)) {
            cs.launch {
                try {
                    if (vm.isResolved) vm.unresolve() else vm.resolve()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    LOG.warn("${GiteaBundle.message(errorKey)} (thread ${vm.id})", e)
                    NotificationGroupManager.getInstance()
                        .getNotificationGroup("Gitea")
                        .createNotification(GiteaBundle.message(labelKey), GiteaBundle.message(errorKey), NotificationType.ERROR)
                        .notify(project)
                }
            }
        }
    }

    private fun createCommentPanel(
        project: Project,
        cs: CoroutineScope,
        discussionsVm: GiteaPRDiscussionsViewModels,
        vm: GiteaPRCommentViewModel,
        /** Tags shown after the author and date, e.g. Resolved/Outdated on a thread's first comment. */
        tags: List<JComponent> = emptyList(),
    ): JComponent {
        val suggestion = if (vm.comment.path != null) vm.body?.let { GiteaSuggestionUtil.detect(it) } else null
        val displayBody = suggestion?.let { GiteaSuggestionUtil.stripSuggestion(vm.body!!) } ?: vm.body
        val (bodyComponent, actionsPanel) = commentBodyAndActions(project, cs, discussionsVm, vm, displayBody)
        val content = withSuggestion(cs, project, vm.comment.path, bodyComponent, displayBody.isNullOrBlank(), suggestion)
        return CodeReviewChatItemUIUtil.build(
            ComponentType.COMPACT,
            { size -> discussionsVm.avatars.getIcon(vm.author, size) },
            content,
        ) {
            val title = titleTextPane(authorName(vm.author), vm.author?.htmlUrl, vm.createdAt, vm.comment.isEdited)
            val header = if (tags.isEmpty()) title else HorizontalListPanel(CodeReviewCommentUIUtil.Title.HORIZONTAL_GAP).apply {
                add(title)
                tags.forEach(::add)
            }
            withHeader(header, actionsPanel)
        }
    }

    /** [CodeReviewTimelineUIUtil.createTitleTextPane] plus a small "edited" suffix when [edited] —
     * matches [com.github.jpmand.idea.plugin.gitea.pullrequest.ui.timeline.GiteaPRTimelineItemComponentFactory.titleTextPane]. */
    private fun titleTextPane(name: String, url: String?, timestamp: Date?, edited: Boolean): JComponent {
        val titlePane = CodeReviewTimelineUIUtil.createTitleTextPane(name, url, timestamp ?: Date())
        if (!edited) return titlePane
        return HorizontalListPanel(CodeReviewCommentUIUtil.Title.HORIZONTAL_GAP).apply {
            add(titlePane)
            add(JLabel(GiteaBundle.message("pull.request.timeline.comment.edited")).apply {
                foreground = UIUtil.getContextHelpForeground()
                font = JBFont.small()
            })
        }
    }

    private fun authorName(user: GiteaUser?): String = user?.let { it.fullName ?: it.login } ?: GiteaBundle.message("pull.request.comment.author.unknown")

    /**
     * Renders a comment's body, swappable in-place for an editor when its own author clicks Edit
     * — plus, only for the signed-in user's own comments, an edit/delete actions row using the
     * same platform helpers the Timeline's `commentBodyAndActions` uses
     * ([CodeReviewCommentUIUtil.createEditButton]/[CodeReviewCommentUIUtil.createDeleteCommentIconButton]).
     */
    private fun commentBodyAndActions(
        project: Project,
        cs: CoroutineScope,
        discussionsVm: GiteaPRDiscussionsViewModels,
        vm: GiteaPRCommentViewModel,
        displayBody: String? = vm.body,
    ): Pair<JComponent, JComponent?> {
        val bodyArea = commentBodyPane(cs, displayBody)
        val login = discussionsVm.currentUserLogin
        if (login == null || vm.author?.login != login) return bodyArea to null

        val editVmFlow = MutableStateFlow<CodeReviewTextEditingViewModel?>(null)
        val bodyComponent = EditableComponentFactory.wrapTextComponent(cs, bodyArea, editVmFlow)

        val actionsPanel = HorizontalListPanel(CodeReviewCommentUIUtil.Actions.HORIZONTAL_GAP).apply {
            add(CodeReviewCommentUIUtil.createEditButton {
                val editVm = InlineCommentEditViewModel(project, cs, vm.body.orEmpty(), vm.id, discussionsVm) { editVmFlow.value = null }
                editVmFlow.value = editVm
                editVm.requestFocus()
            })
            add(CodeReviewCommentUIUtil.createDeleteCommentIconButton {
                cs.launch {
                    try {
                        discussionsVm.deleteComment(vm.id)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        LOG.warn("Couldn't delete review comment ${vm.id}", e)
                        NotificationGroupManager.getInstance()
                            .getNotificationGroup("Gitea")
                            .createNotification(
                                GiteaBundle.message("pull.request.action.delete.comment.error"),
                                e.localizedMessage.orEmpty(),
                                NotificationType.ERROR,
                            )
                            .notify(project)
                    }
                }
            })
        }
        return bodyComponent to actionsPanel
    }

    private class InlineCommentEditViewModel(
        project: Project,
        cs: CoroutineScope,
        initialText: String,
        private val commentId: Long,
        private val discussionsVm: GiteaPRDiscussionsViewModels,
        private val onDone: () -> Unit,
    ) : CodeReviewSubmittableTextViewModelBase(project, cs, initialText), CodeReviewTextEditingViewModel {
        override fun save() {
            submit { newBody ->
                try {
                    discussionsVm.editComment(commentId, newBody)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    // Shown in the field by the platform; logged here so it isn't lost.
                    LOG.warn("Couldn't edit review comment $commentId", e)
                    throw e
                }
                onDone()
            }
        }

        override fun stopEditing() = onDone()
    }
}
