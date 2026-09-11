package com.github.jpmand.idea.plugin.gitea.pullrequest.ui.editor

import com.github.jpmand.idea.plugin.gitea.pullrequest.review.GiteaPRCommentViewModel
import com.github.jpmand.idea.plugin.gitea.pullrequest.review.GiteaPRThreadViewModel
import com.github.jpmand.idea.plugin.gitea.util.GiteaBundle
import com.intellij.collaboration.ui.codereview.editor.CodeReviewComponentInlayRenderer
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.editor.ComponentInlayRenderer
import com.intellij.openapi.project.Project
import com.intellij.ui.components.ActionLink
import com.intellij.util.ui.JBUI
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.awt.FlowLayout
import java.text.SimpleDateFormat
import javax.swing.*

/** Existing comment threads, read-only display plus resolve/unresolve. Comment composition
 * (NewComment/DraftComment) is a separate, still-unimplemented feature. */
@Suppress("UnstableApiUsage")
object GiteaPRInlayComponentsFactory {

    fun createRenderer(project: Project, cs: CoroutineScope, model: GiteaPRInlayModel): ComponentInlayRenderer<JComponent> =
        when (model) {
            is GiteaPRInlayModel.Thread -> CodeReviewComponentInlayRenderer(createThreadPanel(project, cs, model.vm))
        }

    private fun createThreadPanel(project: Project, cs: CoroutineScope, vm: GiteaPRThreadViewModel): JComponent {
        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
        panel.border = JBUI.Borders.empty(8, 12)

        for (commentVm in vm.commentVMs) {
            panel.add(createCommentPanel(commentVm))
            panel.add(Box.createVerticalStrut(JBUI.scale(6)))
        }

        panel.add(createResolveRow(project, cs, vm))

        return panel
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

    private fun createCommentPanel(vm: GiteaPRCommentViewModel): JComponent {
        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
        val authorText = vm.author?.login ?: "unknown"
        val dateText = vm.createdAt?.let { SimpleDateFormat("yyyy-MM-dd").format(it) } ?: ""
        val header = JLabel("<html><b>$authorText</b>&nbsp;&nbsp;<span color='gray'>$dateText</span></html>")
        panel.add(header)
        val bodyArea = JTextArea(vm.body ?: "").apply {
            isEditable = false
            lineWrap = true
            wrapStyleWord = true
            isOpaque = false
            border = JBUI.Borders.empty(4, 0)
        }
        panel.add(bodyArea)
        return panel
    }
}
