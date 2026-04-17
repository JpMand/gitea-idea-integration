package com.github.jpmand.idea.plugin.gitea.pullrequest.ui.list

import com.github.jpmand.idea.plugin.gitea.api.models.GiteaPullRequest
import com.github.jpmand.idea.plugin.gitea.api.rest.models.GiteaStateEnum
import com.github.jpmand.idea.plugin.gitea.pullrequest.data.GiteaPRRepository
import com.intellij.collaboration.ui.codereview.list.ReviewListViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.swing.DefaultListModel

@Suppress("UnstableApiUsage")
class GiteaPRListViewModel(
    private val cs: CoroutineScope,
    private val repository: GiteaPRRepository,
) : ReviewListViewModel {

    val searchVm = GiteaPRListSearchPanelViewModel(cs)

    private val _listModel = DefaultListModel<GiteaPullRequest>()
    val listModel: DefaultListModel<GiteaPullRequest> = _listModel

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<Throwable?>(null)
    val error: StateFlow<Throwable?> = _error.asStateFlow()

    /** Bumped by [refresh] to re-run the current filter without changing it. */
    private val _refreshTrigger = MutableStateFlow(0L)

    init {
        cs.launch(Dispatchers.IO) {
            searchVm.searchState
                .combine(_refreshTrigger) { filter, _ -> filter }
                .collectLatest { filter -> loadPRs(filter) }
        }
    }

    private suspend fun loadPRs(filter: GiteaPRListSearchValue) {
        _isLoading.value = true
        _error.value = null
        try {
            val stateEnum = when (filter.state) {
                GiteaPRListSearchValue.State.OPEN -> GiteaStateEnum.OPEN
                GiteaPRListSearchValue.State.CLOSED -> GiteaStateEnum.CLOSED
                GiteaPRListSearchValue.State.ALL -> GiteaStateEnum.ALL
            }
            val prs = repository.loadPullRequests(stateEnum, page = null, limit = 50)
            val query = filter.searchQuery
            val filtered = if (query.isNullOrBlank()) prs
            else prs.filter { pr ->
                pr.title.contains(query, ignoreCase = true) ||
                        "#${pr.number}".contains(query, ignoreCase = true)
            }
            withContext(Dispatchers.Main) {
                _listModel.clear()
                filtered.forEach { _listModel.addElement(it) }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _error.value = e
        } finally {
            withContext(NonCancellable) {
                _isLoading.value = false
            }
        }
    }

    override fun refresh() {
        _refreshTrigger.value = System.currentTimeMillis()
    }
}
