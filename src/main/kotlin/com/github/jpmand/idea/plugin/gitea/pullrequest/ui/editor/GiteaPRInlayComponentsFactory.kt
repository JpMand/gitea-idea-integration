package com.github.jpmand.idea.plugin.gitea.pullrequest.ui.editor

import com.github.jpmand.idea.plugin.gitea.api.models.GiteaPRDraftComment
import com.github.jpmand.idea.plugin.gitea.api.models.GiteaUser
import com.github.jpmand.idea.plugin.gitea.pullrequest.review.GiteaPRCommentViewModel
import com.github.jpmand.idea.plugin.gitea.pullrequest.review.GiteaPRDiscussionsViewModels
import com.github.jpmand.idea.plugin.gitea.pullrequest.review.GiteaPRThreadViewModel
import com.github.jpmand.idea.plugin.gitea.pullrequest.review.GiteaSuggestionUtil
import com.github.jpmand.idea.plugin.gitea.pullrequest.ui.comment.GiteaPRCommentFieldFactory
import com.github.jpmand.idea.plugin.gitea.pullrequest.ui.comment.GiteaPRSubmittableTextViewModel
import com.github.jpmand.idea.plugin.gitea.pullrequest.ui.createSuggestionDiffBox
import com.github.jpmand.idea.plugin.gitea.pullrequest.ui.createThreadCommentsPanel
import com.github.jpmand.idea.plugin.gitea.pullrequest.ui.withSuggestion
import com.github.jpmand.idea.plugin.gitea.util.GiteaBundle
import com.intellij.collaboration.ui.CollaborationToolsUIUtil
import com.intellij.collaboration.ui.EditableComponentFactory
import com.intellij.collaboration.ui.HorizontalListPanel
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
import com.intellij.openapi.editor.ComponentInlayRenderer
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.project.Project
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
import java.awt.FlowLayout
import java.util.Date
import javax.swing.*

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

    fun createRenderer(
        project: Project,
        cs: CoroutineScope,
        model: GiteaPRInlayModel,
        discussionsVm: GiteaPRDiscussionsViewModels,
    ): ComponentInlayRenderer<JComponent> =
        when (model) {
            is GiteaPRInlayModel.Thread -> {
                val card = CodeReviewCommentUIUtil.createEditorInlayPanel(createThreadPanel(project, cs, model.vm, discussionsVm))
                installHoverAnchorHighlight(card, model.editor, model.editorLineIdx)
                CodeReviewComponentInlayRenderer(withInlayMargin(card))
            }
            is GiteaPRInlayModel.NewComment -> CodeReviewComponentInlayRenderer(
                withInlayMargin(CodeReviewCommentUIUtil.createEditorInlayPanel(createNewCommentPanel(project, cs, model.vm, discussionsVm))),
            )
        }

    /** Breathing room between the rounded card and the surrounding code lines — the inlay
     * machinery itself adds none (it's just a component appended after a line's end offset). */
    private fun withInlayMargin(card: JComponent): JComponent =
        Wrapper(card).apply { border = JBUI.Borders.empty(CodeReviewChatItemUIUtil.THREAD_TOP_MARGIN, 0) }

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
        val commentsPanel = createThreadCommentsPanel(vm.commentVMs) { commentVm ->
            createCommentPanel(project, cs, discussionsVm, commentVm)
        }

        // 4/CodeReviewCommentUIUtil.INLAY_PADDING(=10) matches ComponentType.COMPACT's own padding
        // insets, so these rows line up with the comment rows above them. The vertical gap between
        // rows (comments/resolve/reply) gets a little breathing room too — COMPACT's own 4px
        // top/bottom inset per row reads as cramped when several rows stack directly.
        val panel = VerticalListPanel(4)
        panel.add(commentsPanel)
        panel.add(createResolveRow(project, cs, vm).apply { border = JBUI.Borders.empty(4, 10) })
        // Outdated threads (anchored to a diff that's no longer current) can't be replied to.
        if (!vm.isOutdated) {
            panel.add(replyComposer(project, cs, discussionsVm, vm.lastCommentId).apply { border = JBUI.Borders.empty(4, 10) })
        }

        return panel
    }

    /**
     * A "Reply" link that swaps in a comment composer (same [GiteaPRCommentFieldFactory] machinery
     * the Timeline's thread-reply/leave-a-comment fields use) when clicked, and swaps back once
     * submitted. Stays hidden while [GiteaPRDiscussionsViewModels.currentUser] hasn't resolved yet.
     */
    private fun replyComposer(
        project: Project,
        cs: CoroutineScope,
        discussionsVm: GiteaPRDiscussionsViewModels,
        threadId: Long,
    ): JComponent {
        val wrapper = Wrapper()
        cs.launch {
            discussionsVm.currentUser.collect { user ->
                wrapper.setContent(user?.let { replyLink(project, cs, wrapper, discussionsVm, threadId, it) })
                wrapper.revalidate()
                wrapper.repaint()
            }
        }
        return wrapper
    }

    private fun replyLink(
        project: Project,
        cs: CoroutineScope,
        wrapper: Wrapper,
        discussionsVm: GiteaPRDiscussionsViewModels,
        threadId: Long,
        user: GiteaUser,
    ): JComponent =
        ActionLink(GiteaBundle.message("pull.request.action.reply")) {
            val replyVm = GiteaPRSubmittableTextViewModel(project, cs) { body ->
                discussionsVm.replyToThread(threadId, body)
                wrapper.setContent(replyLink(project, cs, wrapper, discussionsVm, threadId, user))
                wrapper.revalidate()
                wrapper.repaint()
            }
            wrapper.setContent(
                GiteaPRCommentFieldFactory.create(cs, replyVm, discussionsVm.avatars, user, discussionsVm.mentionCandidates),
            )
            wrapper.revalidate()
            wrapper.repaint()
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
        )
        val suggestion = vm.suggestion ?: return commentField
        return VerticalListPanel(4).apply {
            add(createSuggestionDiffBox(cs, project, suggestion))
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
        val bodyArea = JTextArea(displayBody).apply {
            isEditable = false
            lineWrap = true
            wrapStyleWord = true
            isOpaque = false
            border = JBUI.Borders.empty(4, 0)
        }
        val editVmFlow = MutableStateFlow<CodeReviewTextEditingViewModel?>(null)
        val textComponent = EditableComponentFactory.wrapTextComponent(cs, bodyArea, editVmFlow)
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
            withHeader(CollaborationToolsUIUtil.createTagLabel(GiteaBundle.message("pull.request.diff.draft.badge")), actionsPanel)
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

    private fun createResolveRow(project: Project, cs: CoroutineScope, vm: GiteaPRThreadViewModel): JComponent {
        val row = JPanel(FlowLayout(FlowLayout.LEFT, 0, 0))
        row.isOpaque = false
        val labelKey = if (vm.isResolved) "pull.request.action.unresolve.thread" else "pull.request.action.resolve.thread"
        val errorKey = if (vm.isResolved) "pull.request.action.unresolve.thread.error" else "pull.request.action.resolve.thread.error"
        row.add(JButton(GiteaBundle.message(labelKey)).apply {
            addActionListener {
                cs.launch {
                    try {
                        if (vm.isResolved) vm.unresolve() else vm.resolve()
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

    private fun createCommentPanel(
        project: Project,
        cs: CoroutineScope,
        discussionsVm: GiteaPRDiscussionsViewModels,
        vm: GiteaPRCommentViewModel,
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
            withHeader(titleTextPane(authorName(vm.author), vm.author?.htmlUrl, vm.createdAt, vm.comment.isEdited), actionsPanel)
        }
    }

    /** [CodeReviewTimelineUIUtil.createTitleTextPane] plus a small "edited" suffix when [edited] —
     * matches [com.github.jpmand.idea.plugin.gitea.pullrequest.ui.timeline.GiteaPRTimelineItemComponentFactory.titleTextPane]. */
    private fun titleTextPane(name: String, url: String?, timestamp: Date?, edited: Boolean): JComponent {
        val titlePane = CodeReviewTimelineUIUtil.createTitleTextPane(name, url, timestamp ?: Date())
        if (!edited) return titlePane
        return HorizontalListPanel(4).apply {
            add(titlePane)
            add(JLabel(GiteaBundle.message("pull.request.timeline.comment.edited")).apply {
                foreground = UIUtil.getContextHelpForeground()
                font = JBFont.small()
            })
        }
    }

    private fun authorName(user: GiteaUser?): String = user?.let { it.fullName ?: it.login } ?: "unknown"

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
        val bodyArea = JTextArea(displayBody ?: "").apply {
            isEditable = false
            lineWrap = true
            wrapStyleWord = true
            isOpaque = false
            border = JBUI.Borders.empty(4, 0)
        }
        if (vm.author?.login != discussionsVm.currentUserLogin) return bodyArea to null

        val editVmFlow = MutableStateFlow<CodeReviewTextEditingViewModel?>(null)
        val bodyComponent = EditableComponentFactory.wrapTextComponent(cs, bodyArea, editVmFlow)

        val actionsPanel = HorizontalListPanel(CodeReviewCommentUIUtil.Actions.HORIZONTAL_GAP).apply {
            add(CodeReviewCommentUIUtil.createEditButton {
                val editVm = InlineCommentEditViewModel(project, cs, vm.body.orEmpty(), vm.id, discussionsVm) { editVmFlow.value = null }
                editVmFlow.value = editVm
                editVm.requestFocus()
            })
            add(CodeReviewCommentUIUtil.createDeleteCommentIconButton {
                cs.launch { discussionsVm.deleteComment(vm.id) }
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
                discussionsVm.editComment(commentId, newBody)
                onDone()
            }
        }

        override fun stopEditing() = onDone()
    }
}
