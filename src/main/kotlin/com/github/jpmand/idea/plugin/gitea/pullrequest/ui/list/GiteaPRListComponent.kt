package com.github.jpmand.idea.plugin.gitea.pullrequest.ui.list

import com.github.jpmand.idea.plugin.gitea.api.models.GiteaPullRequest
import com.github.jpmand.idea.plugin.gitea.pullrequest.ui.GiteaPRRefreshAction
import com.github.jpmand.idea.plugin.gitea.util.GiteaBundle
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.asContextElement
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.awt.BorderLayout
import java.awt.CardLayout
import javax.swing.DefaultListModel
import javax.swing.JPanel
import javax.swing.ListSelectionModel

/**
 * Full list panel: toolbar (refresh + filters) + content area that switches between
 * loading / empty / error / list cards based on [GiteaPRListViewModel.listState].
 */
class GiteaPRListComponent(
    @Suppress("unused") project: Project,
    cs: CoroutineScope,
    private val vm: GiteaPRListViewModel,
    @Suppress("unused") filters: GiteaPRListFiltersModel
) : JPanel(BorderLayout()) {

    companion object {
        private const val CARD_LOADING = "loading"
        private const val CARD_EMPTY = "empty"
        private const val CARD_ERROR = "error"
        private const val CARD_LIST = "list"
    }

    private val listModel = DefaultListModel<GiteaPullRequest>()
    private val jbList = JBList(listModel).apply {
        cellRenderer = GiteaPRListItemComponent()
        selectionMode = ListSelectionModel.SINGLE_SELECTION
        addListSelectionListener { e ->
            if (!e.valueIsAdjusting) {
                selectedValue?.let { vm.select(it) }
            }
        }
    }

    private val cardPanel = JPanel(CardLayout())
    private val cardLayout = cardPanel.layout as CardLayout
    private val errorLabel = JBLabel("", JBLabel.CENTER)

    init {
        // ── Toolbar ────────────────────────────────────────────────────
        val group = DefaultActionGroup().apply {
            add(GiteaPRRefreshAction(vm))
        }
        val toolbar = ActionManager.getInstance()
            .createActionToolbar("Gitea.PullRequest.ToolWindow", group, true)
            .also { it.targetComponent = this }
        add(toolbar.component, BorderLayout.NORTH)

        // ── State cards ────────────────────────────────────────────────
        cardPanel.add(buildCentredLabel(GiteaBundle.message("pullrequest.list.loading")), CARD_LOADING)
        cardPanel.add(buildCentredLabel(GiteaBundle.message("pullrequest.list.empty")), CARD_EMPTY)
        cardPanel.add(JPanel(BorderLayout()).also { it.add(errorLabel, BorderLayout.CENTER) }, CARD_ERROR)
        cardPanel.add(JBScrollPane(jbList), CARD_LIST)
        add(cardPanel, BorderLayout.CENTER)

        // ── Observe VM state on EDT ────────────────────────────────────
        cs.launch(Dispatchers.EDT + ModalityState.any().asContextElement()) {
            vm.listState.collect { state ->
                when (state) {
                    is GiteaPRListViewModel.ListState.Loading -> {
                        listModel.clear()
                        cardLayout.show(cardPanel, CARD_LOADING)
                    }
                    is GiteaPRListViewModel.ListState.Empty -> {
                        listModel.clear()
                        cardLayout.show(cardPanel, CARD_EMPTY)
                    }
                    is GiteaPRListViewModel.ListState.Error -> {
                        listModel.clear()
                        errorLabel.text = "${GiteaBundle.message("pullrequest.list.error")}: ${state.message}"
                        cardLayout.show(cardPanel, CARD_ERROR)
                    }
                    is GiteaPRListViewModel.ListState.Items -> {
                        listModel.clear()
                        state.prs.forEach { listModel.addElement(it) }
                        cardLayout.show(cardPanel, CARD_LIST)
                    }
                }
                revalidate()
                repaint()
            }
        }
    }

    private fun buildCentredLabel(text: String): JPanel =
        JPanel(BorderLayout()).also { it.add(JBLabel(text, JBLabel.CENTER), BorderLayout.CENTER) }
}