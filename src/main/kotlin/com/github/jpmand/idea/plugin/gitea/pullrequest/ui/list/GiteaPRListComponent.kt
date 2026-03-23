package com.github.jpmand.idea.plugin.gitea.pullrequest.ui.list

import com.github.jpmand.idea.plugin.gitea.api.models.GiteaPullRequest
import com.github.jpmand.idea.plugin.gitea.api.rest.models.GiteaStateEnum
import com.github.jpmand.idea.plugin.gitea.pullrequest.ui.GiteaPRRefreshAction
import com.github.jpmand.idea.plugin.gitea.util.GiteaBundle
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.asContextElement
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.SearchTextField
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.awt.BorderLayout
import java.awt.CardLayout
import java.awt.event.ItemEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.DefaultListModel
import javax.swing.JPanel
import javax.swing.ListSelectionModel
import javax.swing.event.DocumentEvent

/**
 * Full list panel: filter bar (search + state) at top + content area that switches between
 * loading / empty / error / list cards based on [GiteaPRListViewModel.listState].
 *
 * Refresh is available via right-click context menu on the list.
 */
class GiteaPRListComponent(
    @Suppress("unused") project: Project,
    cs: CoroutineScope,
    private val vm: GiteaPRListViewModel,
    private val filters: GiteaPRListFiltersModel
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
        // ── Filter bar ─────────────────────────────────────────────────────
        val stateCombo = ComboBox(arrayOf(GiteaStateEnum.OPEN, GiteaStateEnum.CLOSED, GiteaStateEnum.ALL)).apply {
            renderer = SimpleListCellRenderer.create { label, value, _ ->
                label.text = when (value) {
                    GiteaStateEnum.OPEN -> GiteaBundle.message("pullrequest.list.filter.state.open")
                    GiteaStateEnum.CLOSED -> GiteaBundle.message("pullrequest.list.filter.state.closed")
                    GiteaStateEnum.ALL -> GiteaBundle.message("pullrequest.list.filter.state.all")
                    null -> ""
                }
            }
            addItemListener { e ->
                if (e.stateChange == ItemEvent.SELECTED) {
                    (e.item as? GiteaStateEnum)?.let { filters.state.value = it }
                }
            }
        }

        val searchField = SearchTextField(false).apply {
            textEditor.emptyText.setText(GiteaBundle.message("pullrequest.list.search.placeholder"))
            addDocumentListener(object : DocumentAdapter() {
                override fun textChanged(e: DocumentEvent) {
                    filters.searchText.value = text
                }
            })
        }

        val filterBar = JPanel(BorderLayout(JBUI.scale(8), 0)).apply {
            border = JBUI.Borders.empty(4, 8, 4, 8)
            add(stateCombo, BorderLayout.WEST)
            add(searchField, BorderLayout.CENTER)
        }
        add(filterBar, BorderLayout.NORTH)

        // ── Right-click context menu ────────────────────────────────────────
        jbList.addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) = maybeShowPopup(e)
            override fun mouseReleased(e: MouseEvent) = maybeShowPopup(e)

            fun maybeShowPopup(e: MouseEvent) {
                if (!e.isPopupTrigger) return
                // Select the item under the cursor before showing the menu
                val idx = jbList.locationToIndex(e.point)
                if (idx >= 0) jbList.selectedIndex = idx

                val group = DefaultActionGroup().apply {
                    add(GiteaPRRefreshAction(vm))
                }
                val popup = ActionManager.getInstance()
                    .createActionPopupMenu("Gitea.PullRequest.List.Context", group)
                popup.setTargetComponent(jbList)
                popup.component.show(jbList, e.x, e.y)
            }
        })

        // ── State cards ────────────────────────────────────────────────────
        cardPanel.add(buildCentredLabel(GiteaBundle.message("pullrequest.list.loading")), CARD_LOADING)
        cardPanel.add(buildCentredLabel(GiteaBundle.message("pullrequest.list.empty")), CARD_EMPTY)
        cardPanel.add(JPanel(BorderLayout()).also { it.add(errorLabel, BorderLayout.CENTER) }, CARD_ERROR)
        cardPanel.add(JBScrollPane(jbList), CARD_LIST)
        add(cardPanel, BorderLayout.CENTER)

        // ── Observe VM state on EDT ────────────────────────────────────────
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