package com.github.jpmand.idea.plugin.gitea.pullrequest.ui.details

import com.github.jpmand.idea.plugin.gitea.api.rest.pr.GiteaPRFileStatusEnum
import com.github.jpmand.idea.plugin.gitea.pullrequest.diff.GiteaPRDiffFileViewModel
import com.github.jpmand.idea.plugin.gitea.pullrequest.diff.GiteaPRDiffViewModel
import com.intellij.openapi.vcs.FileStatus
import com.intellij.ui.ColoredListCellRenderer
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.components.JBList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.swing.DefaultListModel
import javax.swing.JComponent
import javax.swing.JList

/**
 * A pragmatic file list for the PR's changed files — not the platform's full
 * `AsyncChangesTree`/`RefComparisonChange` machinery, which expects VCS-resolved local-git
 * changes and doesn't fit this plugin's Gitea-REST-API-backed diff content model (see
 * PR_REVIEW_MIGRATION_PLAN.md). Sourced from data [GiteaPRDiffViewModel] already loads — no new
 * loading logic here. Clicking a row retargets the PR's existing diff view to that file and
 * opens/focuses its editor tab.
 */
@Suppress("UnstableApiUsage")
object GiteaPRChangesBrowserComponentFactory {

    fun create(cs: CoroutineScope, diffVm: GiteaPRDiffViewModel, onFileOpened: () -> Unit): JComponent {
        val model = DefaultListModel<GiteaPRDiffFileViewModel>()
        val list = JBList(model).apply {
            cellRenderer = Renderer()
        }

        list.addListSelectionListener { e ->
            if (!e.valueIsAdjusting) {
                val idx = list.selectedIndex
                if (idx >= 0) {
                    diffVm.showChange(idx, null)
                    onFileOpened()
                }
            }
        }

        cs.launch(Dispatchers.Main.immediate) {
            diffVm.changes.collect { computed ->
                val files = computed?.result?.getOrNull()?.selectedChanges?.list.orEmpty()
                model.clear()
                files.forEach { model.addElement(it) }
            }
        }

        return ScrollPaneFactory.createScrollPane(list, true)
    }

    private class Renderer : ColoredListCellRenderer<GiteaPRDiffFileViewModel>() {
        override fun customizeCellRenderer(
            list: JList<out GiteaPRDiffFileViewModel>,
            value: GiteaPRDiffFileViewModel?,
            index: Int,
            selected: Boolean,
            hasFocus: Boolean,
        ) {
            val file = value?.file ?: return
            val fileStatus = when (file.status) {
                GiteaPRFileStatusEnum.ADDED -> FileStatus.ADDED
                GiteaPRFileStatusEnum.DELETED -> FileStatus.DELETED
                else -> FileStatus.MODIFIED
            }
            append(file.filename, SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, fileStatus.color))
            if (file.additions > 0 || file.deletions > 0) {
                append("  +${file.additions} -${file.deletions}", SimpleTextAttributes.GRAYED_ATTRIBUTES)
            }
        }
    }
}
