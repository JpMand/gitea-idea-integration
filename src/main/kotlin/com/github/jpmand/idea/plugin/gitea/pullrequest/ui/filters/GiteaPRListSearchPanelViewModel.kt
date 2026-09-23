package com.github.jpmand.idea.plugin.gitea.pullrequest.ui.filters

import com.github.jpmand.idea.plugin.gitea.api.models.GiteaLabel
import com.github.jpmand.idea.plugin.gitea.api.models.GiteaUser
import com.github.jpmand.idea.plugin.gitea.pullrequest.data.GiteaPRRepository
import com.github.jpmand.idea.plugin.gitea.pullrequest.ui.list.GiteaPRListQuickFilter
import com.intellij.collaboration.ui.codereview.list.search.ReviewListSearchHistoryModel
import com.intellij.collaboration.ui.codereview.list.search.ReviewListSearchPanelViewModelBase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.shareIn

@Suppress("UnstableApiUsage")
class GiteaPRListSearchPanelViewModel(
    scope: CoroutineScope,
    private val repository: GiteaPRRepository,
) : ReviewListSearchPanelViewModelBase<GiteaPRListSearchValue, GiteaPRListQuickFilter>(
    scope,
    historyModel = InMemorySearchHistoryModel(),
    emptySearch = GiteaPRListSearchValue.EMPTY,
    defaultFilter = GiteaPRListSearchValue.DEFAULT,
) {

    override fun GiteaPRListSearchValue.withQuery(query: String?) = copy(searchQuery = query)

    override val quickFilters: List<GiteaPRListQuickFilter> = listOf(
        GiteaPRListQuickFilter.Open,
        GiteaPRListQuickFilter.Closed,
        GiteaPRListQuickFilter.All,
    )

    // ── Fixed filter facets (two-way views onto searchState) ────────────────
    // A null facet value means "no filter". For state, OPEN is the implicit default, so it reads
    // back as null (the dropdown shows nothing selected) and clearing the dropdown restores OPEN.

    val stateFilter: MutableStateFlow<GiteaPRListSearchValue.State?> = searchState.partialState(
        { it.state.takeIf { s -> s != GiteaPRListSearchValue.State.OPEN } },
        { copy(state = it ?: GiteaPRListSearchValue.State.OPEN) },
    )

    val labelFilter: MutableStateFlow<String?> = searchState.partialState(
        { it.label },
        { copy(label = it) },
    )

    val authorFilter: MutableStateFlow<String?> = searchState.partialState(
        { it.author },
        { copy(author = it) },
    )

    val sortFilter: MutableStateFlow<GiteaPRListSearchValue.Sort?> = searchState.partialState(
        { it.sort },
        { copy(sort = it) },
    )

    // ── Lazily-loaded dropdown option sources (only fetched once the popup is opened) ────────

    val labelOptions: Flow<Result<List<GiteaLabel>>> =
        flow { emit(runCatching { repository.loadLabels() }) }
            .shareIn(scope, SharingStarted.Lazily, replay = 1)

    val authorOptions: Flow<Result<List<GiteaUser>>> =
        flow { emit(runCatching { repository.loadPossibleAuthors() }) }
            .shareIn(scope, SharingStarted.Lazily, replay = 1)
}

@Suppress("UnstableApiUsage")
private class InMemorySearchHistoryModel : ReviewListSearchHistoryModel<GiteaPRListSearchValue> {
    private val history = mutableListOf<GiteaPRListSearchValue>()
    override var lastFilter: GiteaPRListSearchValue? = null
    override fun getHistory(): List<GiteaPRListSearchValue> = history.toList()
    override fun add(filter: GiteaPRListSearchValue) {
        history.add(0, filter)
    }
}
