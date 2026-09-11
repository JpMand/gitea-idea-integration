package com.github.jpmand.idea.plugin.gitea.pullrequest.ui.details

import com.github.jpmand.idea.plugin.gitea.api.rest.dto.MergePullRequestOption
import com.github.jpmand.idea.plugin.gitea.util.GiteaBundle
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.ColoredListCellRenderer
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JList
import javax.swing.JPanel

/**
 * Confirms merge method + delete-branch before the write call — mirrors GitHub's merge-button
 * dropdown. "Manually merged" is excluded: it records a merge that already happened elsewhere,
 * not something this dialog performs.
 */
class GiteaPRMergeDialog(project: Project?) : DialogWrapper(project, true) {

    private val methodCombo = JComboBox(
        arrayOf(
            MergePullRequestOption.Do.MERGE,
            MergePullRequestOption.Do.REBASE,
            MergePullRequestOption.Do.REBASEMERGE,
            MergePullRequestOption.Do.SQUASH,
            MergePullRequestOption.Do.FASTFORWARDONLY,
        ),
    ).apply {
        renderer = object : ColoredListCellRenderer<MergePullRequestOption.Do>() {
            override fun customizeCellRenderer(
                list: JList<out MergePullRequestOption.Do>,
                value: MergePullRequestOption.Do,
                index: Int,
                selected: Boolean,
                hasFocus: Boolean,
            ) {
                append(methodLabel(value))
            }
        }
    }

    private val deleteBranchCheckBox = JBCheckBox(GiteaBundle.message("pull.request.merge.dialog.delete.branch"))

    val selectedMethod: MergePullRequestOption.Do get() = methodCombo.selectedItem as MergePullRequestOption.Do
    val deleteBranch: Boolean get() = deleteBranchCheckBox.isSelected

    init {
        title = GiteaBundle.message("pull.request.merge.dialog.title")
        init()
    }

    override fun createCenterPanel(): JComponent =
        JPanel(GridBagLayout()).apply {
            val c = GridBagConstraints().apply {
                gridx = 0; gridy = 0; anchor = GridBagConstraints.WEST; insets = JBUI.insetsBottom(8)
            }
            add(JBLabel(GiteaBundle.message("pull.request.merge.dialog.method")), c)
            c.gridy = 1
            add(methodCombo, c)
            c.gridy = 2
            add(deleteBranchCheckBox, c)
        }

    private fun methodLabel(method: MergePullRequestOption.Do): String = when (method) {
        MergePullRequestOption.Do.MERGE -> GiteaBundle.message("pull.request.merge.method.merge")
        MergePullRequestOption.Do.REBASE -> GiteaBundle.message("pull.request.merge.method.rebase")
        MergePullRequestOption.Do.REBASEMERGE -> GiteaBundle.message("pull.request.merge.method.rebase.merge")
        MergePullRequestOption.Do.SQUASH -> GiteaBundle.message("pull.request.merge.method.squash")
        MergePullRequestOption.Do.FASTFORWARDONLY -> GiteaBundle.message("pull.request.merge.method.fast.forward")
        MergePullRequestOption.Do.MANUALLYMERGED -> method.value
    }
}
