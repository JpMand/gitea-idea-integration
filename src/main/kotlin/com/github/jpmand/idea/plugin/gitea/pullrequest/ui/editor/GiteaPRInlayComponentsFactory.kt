package com.github.jpmand.idea.plugin.gitea.pullrequest.ui.editor

import com.github.jpmand.idea.plugin.gitea.api.models.GiteaUser
import com.github.jpmand.idea.plugin.gitea.pullrequest.review.GiteaPRCommentViewModel
import com.github.jpmand.idea.plugin.gitea.pullrequest.review.GiteaPRDiscussionsViewModels
import com.github.jpmand.idea.plugin.gitea.pullrequest.review.GiteaPRThreadViewModel
import com.github.jpmand.idea.plugin.gitea.pullrequest.ui.comment.GiteaPRCommentFieldFactory
import com.github.jpmand.idea.plugin.gitea.pullrequest.ui.comment.GiteaPRSubmittableTextViewModel
import com.github.jpmand.idea.plugin.gitea.util.GiteaBundle
import com.intellij.collaboration.ui.EditableComponentFactory
import com.intellij.collaboration.ui.HorizontalListPanel
import com.intellij.collaboration.ui.codereview.comment.CodeReviewCommentUIUtil
import com.intellij.collaboration.ui.codereview.comment.CodeReviewSubmittableTextViewModelBase
import com.intellij.collaboration.ui.codereview.comment.CodeReviewTextEditingViewModel
import com.intellij.collaboration.ui.codereview.editor.CodeReviewComponentInlayRenderer
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.editor.ComponentInlayRenderer
import com.intellij.openapi.project.Project
import com.intellij.ui.components.ActionLink
import com.intellij.ui.components.panels.Wrapper
import com.intellij.util.ui.JBUI
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.awt.FlowLayout
import java.text.SimpleDateFormat
import javax.swing.*

/** Existing comment threads, read-only display plus resolve/unresolve/reply. Composing a brand
 * new (not-yet-anchored) line comment is a separate, still-unimplemented feature. */
@Suppress("UnstableApiUsage")
object GiteaPRInlayComponentsFactory {

    fun createRenderer(
        project: Project,
        cs: CoroutineScope,
        model: GiteaPRInlayModel,
        discussionsVm: GiteaPRDiscussionsViewModels,
    ): ComponentInlayRenderer<JComponent> =
        when (model) {
            is GiteaPRInlayModel.Thread -> CodeReviewComponentInlayRenderer(createThreadPanel(project, cs, model.vm, discussionsVm))
        }

    private fun createThreadPanel(
        project: Project,
        cs: CoroutineScope,
        vm: GiteaPRThreadViewModel,
        discussionsVm: GiteaPRDiscussionsViewModels,
    ): JComponent {
        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
        panel.border = JBUI.Borders.empty(8, 12)

        for (commentVm in vm.commentVMs) {
            panel.add(createCommentPanel(project, cs, discussionsVm, commentVm))
            panel.add(Box.createVerticalStrut(JBUI.scale(6)))
        }

        panel.add(createResolveRow(project, cs, vm))
        // Outdated threads (anchored to a diff that's no longer current) can't be replied to.
        if (!vm.isOutdated) panel.add(replyComposer(project, cs, discussionsVm, vm.id))

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

    private fun createResolveRow(project: Project, cs: CoroutineScope, vm: GiteaPRThreadViewModel): JComponent {
        val row = JPanel(FlowLayout(FlowLayout.LEFT, 0, 0))
        row.isOpaque = false
        val labelKey = if (vm.isResolved) "pull.request.action.unresolve.thread" else "pull.request.action.resolve.thread"
        val errorKey = if (vm.isResolved) "pull.request.action.unresolve.thread.error" else "pull.request.action.resolve.thread.error"
        row.add(ActionLink(GiteaBundle.message(labelKey)) {
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
        })
        return row
    }

    private fun createCommentPanel(
        project: Project,
        cs: CoroutineScope,
        discussionsVm: GiteaPRDiscussionsViewModels,
        vm: GiteaPRCommentViewModel,
    ): JComponent {
        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
        val authorText = vm.author?.login ?: "unknown"
        val dateText = vm.createdAt?.let { SimpleDateFormat("yyyy-MM-dd").format(it) } ?: ""
        val editedText = if (vm.comment.isEdited) " " + GiteaBundle.message("pull.request.timeline.comment.edited") else ""
        val header = JLabel("<html><b>$authorText</b>&nbsp;&nbsp;<span color='gray'>$dateText$editedText</span></html>")
        panel.add(header)

        val (bodyComponent, actionsPanel) = commentBodyAndActions(project, cs, discussionsVm, vm)
        panel.add(bodyComponent)
        actionsPanel?.let { panel.add(it) }
        return panel
    }

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
    ): Pair<JComponent, JComponent?> {
        val bodyArea = JTextArea(vm.body ?: "").apply {
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
