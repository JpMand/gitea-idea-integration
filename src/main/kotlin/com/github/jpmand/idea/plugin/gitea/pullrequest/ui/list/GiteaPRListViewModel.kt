package com.github.jpmand.idea.plugin.gitea.pullrequest.ui.list

import com.github.jpmand.idea.plugin.gitea.api.GITEA_PAGE_SIZE
import com.github.jpmand.idea.plugin.gitea.api.models.GiteaPullRequest
import com.github.jpmand.idea.plugin.gitea.api.models.GiteaReview
import com.github.jpmand.idea.plugin.gitea.pullrequest.data.GiteaPRRepository
import com.github.jpmand.idea.plugin.gitea.pullrequest.ui.filters.GiteaPRListSearchPanelViewModel
import com.github.jpmand.idea.plugin.gitea.pullrequest.ui.filters.GiteaPRListSearchValue
import com.intellij.collaboration.ui.codereview.list.ReviewListViewModel
import com.intellij.openapi.diagnostic.logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import javax.swing.DefaultListModel

private val LOG = logger<GiteaPRListViewModel>()

@Suppress("UnstableApiUsage")
class GiteaPRListViewModel(
    private val cs: CoroutineScope,
    private val repository: GiteaPRRepository,
) : ReviewListViewModel {

    val searchVm = GiteaPRListSearchPanelViewModel(cs, repository)

    private val _listModel = DefaultListModel<GiteaPullRequest>()
    val listModel: DefaultListModel<GiteaPullRequest> = _listModel

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<Throwable?>(null)
    val error: StateFlow<Throwable?> = _error.asStateFlow()

    /** Bumped by [refresh] to re-run the current filter without changing it. */
    private val _refreshTrigger = MutableStateFlow(0L)

    /** The filter the list currently shows and how far through its pages it has loaded. */
    private class Paging(val filter: GiteaPRListSearchValue, val labelIds: List<Long>?) {
        var nextPage = 1
        var exhausted = false
    }

    @Volatile
    private var paging: Paging? = null

    @Volatile
    private var moreJob: Job? = null

    init {
        cs.launch(Dispatchers.IO) {
            searchVm.searchState
                .combine(_refreshTrigger) { filter, _ -> filter }
                .collectLatest { filter ->
                    moreJob?.cancelAndJoin()
                    loadFirstPage(filter)
                }
        }
    }

    private suspend fun loadFirstPage(filter: GiteaPRListSearchValue) {
        LOG.debug("Loading pull requests for $filter")
        paging = null
        withLoading {
            // Don't leave the previous filter's results in place while (or if) this one fails —
            // they'd read as a valid (but wrong) answer to the current filter.
            withContext(Dispatchers.Main) { _listModel.clear() }
            // The filter holds the label's name (what the chip shows); Gitea filters by label id.
            val labelIds = filter.label?.let { name ->
                searchVm.labelOptions.first().getOrThrow().filter { it.name == name }.map { it.id }
            }
            // An empty id list means the label no longer exists, so nothing can match it.
            if (labelIds != null && labelIds.isEmpty()) {
                LOG.debug("Label '${filter.label}' no longer exists, nothing to load")
                return@withLoading
            }
            val p = Paging(filter, labelIds)
            paging = p
            loadPages(p)
        }
    }

    /** Loads the next page of the current filter; called by the list as it's scrolled near its end. */
    fun requestMore() {
        val p = paging ?: return
        if (p.exhausted || _isLoading.value || moreJob?.isActive == true) return
        moreJob = cs.launch(Dispatchers.IO) {
            withLoading { loadPages(p) }
        }
    }

    /**
     * Fetches pages of [p] until one adds a row or there are none left. The search query is
     * matched locally, so a page can contribute nothing — and the lazy scroll only asks for more
     * after rows are added, so stopping on such a page would stall the list.
     */
    private suspend fun loadPages(p: Paging) {
        while (!p.exhausted) {
            val prs = repository.loadPullRequests(
                state = p.filter.state.apiValue,
                sort = p.filter.sort?.api,
                labels = p.labelIds,
                poster = p.filter.author,
                page = p.nextPage,
                limit = GITEA_PAGE_SIZE,
            )
            p.nextPage++
            // Only an empty page ends the list: a server's MAX_RESPONSE_ITEMS can cap `limit`
            // below what was asked, so a short page isn't necessarily the last.
            if (prs.isEmpty()) p.exhausted = true
            val matching = prs.filter { p.filter.matchesLocally(it) }
            LOG.debug("Page ${p.nextPage - 1}: ${prs.size} pull requests, ${matching.size} match the search")
            if (matching.isNotEmpty()) {
                withContext(Dispatchers.Main) {
                    if (paging === p) matching.forEach { _listModel.addElement(it) }
                }
                return
            }
        }
    }

    private suspend fun withLoading(load: suspend () -> Unit) {
        _isLoading.value = true
        _error.value = null
        try {
            load()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            LOG.warn("Couldn't load pull requests", e)
            _error.value = e
        } finally {
            withContext(NonCancellable) {
                _isLoading.value = false
            }
        }
    }

    override fun refresh() {
        LOG.debug("Refreshing the pull request list")
        repository.dropSharedLoads()
        // Reviewer states too: rows reload theirs as they're drawn again.
        reviewsCache.clear()
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
                } catch (e: Exception) {
                    LOG.debug("Couldn't load reviews for PR #$prNumber; its row shows no review state", e)
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

/**
 * The parts of the filter Gitea's `/pulls` endpoint can't apply: the search text (matched against
 * the title and `#number`), and the label again, in case a server ignores an unknown `labels` id.
 */
internal fun GiteaPRListSearchValue.matchesLocally(pr: GiteaPullRequest): Boolean {
    val query = searchQuery
    return (query.isNullOrBlank() ||
            pr.title.contains(query, ignoreCase = true) ||
            "#${pr.number}".contains(query, ignoreCase = true)) &&
            (label == null || pr.labels.any { it.name == label })
}
