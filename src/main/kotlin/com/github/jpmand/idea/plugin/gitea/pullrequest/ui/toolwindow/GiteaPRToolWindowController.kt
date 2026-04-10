package com.github.jpmand.idea.plugin.gitea.pullrequest.ui.toolwindow

import com.github.jpmand.idea.plugin.gitea.pullrequest.service.GiteaPullRequestsProjectService
import com.github.jpmand.idea.plugin.gitea.pullrequest.ui.details.GiteaPRDetailsComponent
import com.github.jpmand.idea.plugin.gitea.pullrequest.ui.list.GiteaPRListComponent
import com.github.jpmand.idea.plugin.gitea.pullrequest.ui.list.GiteaPRListFiltersModel
import com.github.jpmand.idea.plugin.gitea.pullrequest.ui.list.GiteaPRListViewModel
import com.github.jpmand.idea.plugin.gitea.util.GiteaBundle
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.asContextElement
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBLabel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.awt.BorderLayout
import java.awt.CardLayout
import javax.swing.JPanel

/**
 * Root UI component for the tool window.
 * Uses a [CardLayout] to switch between Login / NoRepo / PRList / PRDetails panels
 * as [GiteaPRToolWindowViewModel.panelState] changes.
 */
class GiteaPRToolWindowController(
    project: Project,
    cs: CoroutineScope,
    private val vm: GiteaPRToolWindowViewModel
) : JPanel(BorderLayout()) {

    companion object {
        private const val CARD_LOGIN = "login"
        private const val CARD_NO_REPO = "no_repo"
        private const val CARD_LIST = "list"
        private const val CARD_DETAILS = "details"
    }

    private val cards = JPanel(CardLayout())
    private val cardLayout = cards.layout as CardLayout
    private val prService: GiteaPullRequestsProjectService = project.service()

    /** Tracks the prNumber whose details panel is currently installed. */
    private var currentDetailsPrNumber: Int? = null
    private val detailsContainer = JPanel(BorderLayout())

    init {
        val loginPanel = JPanel(BorderLayout()).also {
            it.add(JBLabel(GiteaBundle.message("pullrequest.login.prompt"), JBLabel.CENTER), BorderLayout.CENTER)
        }
        val noRepoPanel = JPanel(BorderLayout()).also {
            it.add(JBLabel(GiteaBundle.message("pullrequest.no.repo"), JBLabel.CENTER), BorderLayout.CENTER)
        }

        val filtersModel = GiteaPRListFiltersModel()
        val listVm = GiteaPRListViewModel(project, cs, vm, filtersModel)
        val listComponent = GiteaPRListComponent(project, cs, listVm, filtersModel)

        cards.add(loginPanel, CARD_LOGIN)
        cards.add(noRepoPanel, CARD_NO_REPO)
        cards.add(listComponent, CARD_LIST)
        cards.add(detailsContainer, CARD_DETAILS)

        add(cards, BorderLayout.CENTER)

        // Observe panel-state changes and swap cards on EDT
        cs.launch(Dispatchers.EDT + ModalityState.any().asContextElement()) {
            vm.panelState.collect { state ->
                when (state) {
                    is GiteaPRToolWindowViewModel.PanelState.LoginPrompt ->
                        cardLayout.show(cards, CARD_LOGIN)

                    is GiteaPRToolWindowViewModel.PanelState.NoRepoDetected ->
                        cardLayout.show(cards, CARD_NO_REPO)

                    is GiteaPRToolWindowViewModel.PanelState.PRList ->
                        cardLayout.show(cards, CARD_LIST)

                    is GiteaPRToolWindowViewModel.PanelState.PRDetails -> {
                        showDetailsPanel(project, cs, state.prNumber)
                        cardLayout.show(cards, CARD_DETAILS)
                    }
                }
                revalidate()
                repaint()
            }
        }
    }

    private fun showDetailsPanel(project: Project, cs: CoroutineScope, prNumber: Int) {
        if (currentDetailsPrNumber == prNumber) return  // already shown
        currentDetailsPrNumber = prNumber
        detailsContainer.removeAll()

        val mapping = prService.activeRepoMappingState.value ?: return
        val owner = mapping.repository.repositoryPath.owner
        val repo = mapping.repository.repositoryPath.repository
        val loader = prService.getDataLoader(owner, repo, prNumber)

        val detailsPanel = GiteaPRDetailsComponent(
            project = project,
            cs = cs,
            prService = prService,
            loader = loader,
            onBack = { vm.backToList() },
        )
        detailsContainer.add(detailsPanel, BorderLayout.CENTER)
        detailsContainer.revalidate()
        detailsContainer.repaint()
    }
}