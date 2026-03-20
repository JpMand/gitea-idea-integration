package com.github.jpmand.idea.plugin.gitea.pullrequest.ui.list

import com.github.jpmand.idea.plugin.gitea.api.models.GiteaPullRequest
import com.github.jpmand.idea.plugin.gitea.api.rest.models.GiteaStateEnum
import com.github.jpmand.idea.plugin.gitea.pullrequest.service.GiteaPullRequestsProjectService
import com.github.jpmand.idea.plugin.gitea.pullrequest.ui.toolwindow.GiteaPRToolWindowViewModel
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * View-model for the pull-request list panel.
 * Collects the raw list from [GiteaPullRequestsProjectService], applies local filters
 * from [GiteaPRListFiltersModel], and exposes a typed [ListState].
 */
class GiteaPRListViewModel(
    project: Project,
    cs: CoroutineScope,
    private val toolWindowVm: GiteaPRToolWindowViewModel,
    filters: GiteaPRListFiltersModel
) {
    sealed interface ListState {
        data object Loading : ListState
        data object Empty : ListState
        data class Error(val message: String) : ListState
        data class Items(val prs: List<GiteaPullRequest>) : ListState
    }

    private data class FilterSnapshot(
        val state: GiteaStateEnum,
        val author: String?,
        val label: String?,
        val assignee: String?
    )

    private val prService: GiteaPullRequestsProjectService = project.service()

    private val filtersFlow: StateFlow<FilterSnapshot> = combine(
        filters.state, filters.author, filters.label, filters.assignee
    ) { s, a, l, ass -> FilterSnapshot(s, a, l, ass) }
        .stateIn(cs, SharingStarted.Eagerly, FilterSnapshot(GiteaStateEnum.OPEN, null, null, null))

    val listState: StateFlow<ListState> =
        prService.pullRequestsState.combine(filtersFlow) { result, f ->
            if (result == null) return@combine ListState.Loading
            result.fold(
                onSuccess = { prs ->
                    val filtered = prs.filter { pr ->
                        (f.state == GiteaStateEnum.ALL || pr.state == f.state.value) &&
                                (f.author == null || pr.author.login.equals(f.author, ignoreCase = true)) &&
                                (f.label == null || pr.labels.any { it.name.equals(f.label, ignoreCase = true) }) &&
                                (f.assignee == null || pr.assignees.any { it.login.equals(f.assignee, ignoreCase = true) })
                    }
                    if (filtered.isEmpty()) ListState.Empty else ListState.Items(filtered)
                },
                onFailure = { ex -> ListState.Error(ex.message ?: "Unknown error") }
            )
        }.stateIn(cs, SharingStarted.Eagerly, ListState.Loading)

    /** Triggers a reload of the pull-request list from the server. */
    fun refresh() {
        prService.refresh()
    }

    /** Navigates to the detail view for the given PR. */
    fun select(pr: GiteaPullRequest) {
        toolWindowVm.selectPR(pr.number)
    }
}