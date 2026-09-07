package com.github.jpmand.idea.plugin.gitea.pullrequest.ui.list

import com.github.jpmand.idea.plugin.gitea.api.models.GiteaPullRequest
import com.github.jpmand.idea.plugin.gitea.api.models.GiteaReview
import com.github.jpmand.idea.plugin.gitea.pullrequest.data.GiteaPRRepository
import com.github.jpmand.idea.plugin.gitea.pullrequest.ui.filters.GiteaPRListSearchPanelViewModel
import com.github.jpmand.idea.plugin.gitea.pullrequest.ui.filters.GiteaPRListSearchValue
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
import java.util.concurrent.ConcurrentHashMap
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
            val prs = repository.loadPullRequests(filter.state.apiValue, page = null, limit = 50)
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

    // ── Reviewers (lazy, cached per PR) ──────────────────────────────────────
    // Loaded on demand by the list's cell renderer rather than eagerly for the whole page,
    // to avoid an N+1 REST call storm on every list load/refresh/filter change.

    private val reviewsCache = ConcurrentHashMap<Long, List<GiteaReview>>()
    private val reviewsLoading = ConcurrentHashMap.newKeySet<Long>()

    /**
     * Returns cached reviews for [prNumber] if already loaded; otherwise kicks off a
     * background load (deduped per PR number) and returns null. Once the load completes,
     * the corresponding row is refreshed in place so its presentation is rebuilt.
     */
    fun reviewsFor(prNumber: Long): List<GiteaReview>? {
        reviewsCache[prNumber]?.let { return it }
        if (reviewsLoading.add(prNumber)) {
            cs.launch(Dispatchers.IO) {
                val reviews = try {
                    repository.loadReviews(prNumber.toInt())
                } catch (e: CancellationException) {
                    reviewsLoading.remove(prNumber)
                    throw e
                } catch (_: Exception) {
                    emptyList()
                }
                reviewsCache[prNumber] = reviews
                reviewsLoading.remove(prNumber)
                withContext(Dispatchers.Main) {
                    val idx = (0 until _listModel.size()).firstOrNull { _listModel[it].number == prNumber }
                    if (idx != null) {
                        _listModel[idx] = _listModel[idx] // re-fires contentsChanged for this row only
                    }
                }
            }
        }
        return null
    }
}
