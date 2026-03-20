package com.github.jpmand.idea.plugin.gitea.pullrequest.ui.toolwindow

import com.github.jpmand.idea.plugin.gitea.authentication.account.GiteaAccountManager
import com.github.jpmand.idea.plugin.gitea.pullrequest.service.GiteaPullRequestsProjectService
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * View-model for the Gitea Pull Requests tool window.
 * Drives which top-level panel is visible and which PR (if any) is selected.
 */
class GiteaPRToolWindowViewModel(
    project: Project,
    cs: CoroutineScope
) {
    sealed interface PanelState {
        data object LoginPrompt : PanelState
        data object NoRepoDetected : PanelState
        data object PRList : PanelState
        data class PRDetails(val prNumber: Int) : PanelState
    }

    private val accountManager: GiteaAccountManager = service()
    private val prService: GiteaPullRequestsProjectService = project.service()

    val selectedPRNumber = MutableStateFlow<Int?>(null)

    val panelState: StateFlow<PanelState> = combine(
        accountManager.accountsState,
        prService.activeRepoMappingState,
        selectedPRNumber
    ) { accounts, mapping, selectedPR ->
        when {
            accounts.isEmpty() -> PanelState.LoginPrompt
            mapping == null -> PanelState.NoRepoDetected
            selectedPR != null -> PanelState.PRDetails(selectedPR)
            else -> PanelState.PRList
        }
    }.stateIn(cs, SharingStarted.Eagerly, PanelState.LoginPrompt)

    fun selectPR(prNumber: Int) {
        selectedPRNumber.value = prNumber
    }

    fun backToList() {
        selectedPRNumber.value = null
    }
}