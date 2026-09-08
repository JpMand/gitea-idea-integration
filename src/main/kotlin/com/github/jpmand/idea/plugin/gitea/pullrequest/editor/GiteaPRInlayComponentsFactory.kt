package com.github.jpmand.idea.plugin.gitea.pullrequest.editor

import com.github.jpmand.idea.plugin.gitea.pullrequest.review.GiteaPRCommentViewModel
import com.github.jpmand.idea.plugin.gitea.pullrequest.review.GiteaPRThreadViewModel
import com.intellij.collaboration.ui.codereview.editor.CodeReviewComponentInlayRenderer
import com.intellij.openapi.editor.ComponentInlayRenderer
import com.intellij.util.ui.JBUI
import kotlinx.coroutines.CoroutineScope
import java.text.SimpleDateFormat
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextArea

/** Milestone-1 (read-only) inlay rendering — existing comment threads only. */
@Suppress("UnstableApiUsage")
object GiteaPRInlayComponentsFactory {

    fun createRenderer(cs: CoroutineScope, model: GiteaPRInlayModel): ComponentInlayRenderer<JComponent> =
        when (model) {
            is GiteaPRInlayModel.Thread -> CodeReviewComponentInlayRenderer(createThreadPanel(model.vm))
        }

    private fun createThreadPanel(vm: GiteaPRThreadViewModel): JComponent {
        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
        panel.border = JBUI.Borders.empty(8, 12)

        for (commentVm in vm.commentVMs) {
            panel.add(createCommentPanel(commentVm))
            panel.add(Box.createVerticalStrut(JBUI.scale(6)))
        }

        // Resolve/Unresolve is a mutation — Milestone 2. Read-only display only here.
        return panel
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
