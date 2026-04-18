package com.github.jpmand.idea.plugin.gitea.pullrequest.editor

import com.github.jpmand.idea.plugin.gitea.pullrequest.review.GiteaPRCommentViewModel
import com.github.jpmand.idea.plugin.gitea.pullrequest.review.GiteaPRNewCommentViewModel
import com.github.jpmand.idea.plugin.gitea.pullrequest.review.GiteaPRThreadViewModel
import com.intellij.collaboration.ui.codereview.editor.CodeReviewComponentInlayRenderer
import com.intellij.openapi.editor.ComponentInlayRenderer
import com.intellij.util.ui.JBUI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTextArea
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

@Suppress("UnstableApiUsage")
object GiteaPRInlayComponentsFactory {

    fun createRenderer(cs: CoroutineScope, model: GiteaPRInlayModel): ComponentInlayRenderer<JComponent> =
        when (model) {
            is GiteaPRInlayModel.Thread -> CodeReviewComponentInlayRenderer(createThreadPanel(cs, model.vm))
            is GiteaPRInlayModel.NewComment -> CodeReviewComponentInlayRenderer(createNewCommentPanel(model.vm))
            is GiteaPRInlayModel.DraftComment -> CodeReviewComponentInlayRenderer(createDraftCommentPanel(model))
        }

    private fun createThreadPanel(cs: CoroutineScope, vm: GiteaPRThreadViewModel): JComponent {
        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
        panel.border = JBUI.Borders.empty(8, 12)

        for (commentVm in vm.commentVMs) {
            panel.add(createCommentPanel(commentVm))
            panel.add(Box.createVerticalStrut(JBUI.scale(6)))
        }

        if (!vm.isOutdated) {
            val resolveLabel = if (vm.isResolved) "Unresolve" else "Resolve"
            val resolveBtn = JButton(resolveLabel).apply {
                addActionListener {
                    cs.launch { if (vm.isResolved) vm.unresolve() else vm.resolve() }
                }
            }
            val btnRow = JPanel()
            btnRow.layout = BoxLayout(btnRow, BoxLayout.X_AXIS)
            btnRow.add(resolveBtn)
            btnRow.add(Box.createHorizontalGlue())
            panel.add(btnRow)
        }
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

    private fun createNewCommentPanel(vm: GiteaPRNewCommentViewModel): JComponent {
        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
        panel.border = JBUI.Borders.empty(8, 12)

        val textArea = JTextArea(3, 40).apply {
            lineWrap = true
            wrapStyleWord = true
        }
        textArea.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent) = vm.updateText(textArea.text)
            override fun removeUpdate(e: DocumentEvent) = vm.updateText(textArea.text)
            override fun changedUpdate(e: DocumentEvent) = vm.updateText(textArea.text)
        })
        panel.add(JScrollPane(textArea))
        panel.add(Box.createVerticalStrut(JBUI.scale(4)))

        val saveBtn = JButton("Add to review").apply {
            addActionListener { if (vm.canSubmit) vm.submit() }
        }
        val cancelBtn = JButton("Cancel").apply {
            addActionListener { vm.cancel() }
        }
        val btnRow = JPanel()
        btnRow.layout = BoxLayout(btnRow, BoxLayout.X_AXIS)
        btnRow.add(saveBtn)
        btnRow.add(Box.createHorizontalStrut(JBUI.scale(8)))
        btnRow.add(cancelBtn)
        btnRow.add(Box.createHorizontalGlue())
        panel.add(btnRow)
        return panel
    }

    private fun createDraftCommentPanel(model: GiteaPRInlayModel.DraftComment): JComponent {
        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
        panel.border = JBUI.Borders.empty(8, 12)

        val header = JLabel("<html><i>Draft</i></html>")
        panel.add(header)
        val bodyArea = JTextArea(model.draft.body).apply {
            isEditable = false
            lineWrap = true
            wrapStyleWord = true
            isOpaque = false
            border = JBUI.Borders.empty(4, 0)
        }
        panel.add(bodyArea)

        val removeBtn = JButton("Remove").apply {
            addActionListener { model.onRemove() }
        }
        val btnRow = JPanel()
        btnRow.layout = BoxLayout(btnRow, BoxLayout.X_AXIS)
        btnRow.add(removeBtn)
        btnRow.add(Box.createHorizontalGlue())
        panel.add(btnRow)
        return panel
    }
}
