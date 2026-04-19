package com.github.jpmand.idea.plugin.gitea.pullrequest.ui.filters

import com.github.jpmand.idea.plugin.gitea.pullrequest.ui.list.GiteaPRListQuickFilter
import com.intellij.collaboration.ui.codereview.list.search.ReviewListSearchHistoryModel
import com.intellij.collaboration.ui.codereview.list.search.ReviewListSearchPanelViewModelBase
import kotlinx.coroutines.CoroutineScope

@Suppress("UnstableApiUsage")
class GiteaPRListSearchPanelViewModel(scope: CoroutineScope) :
    ReviewListSearchPanelViewModelBase<GiteaPRListSearchValue, GiteaPRListQuickFilter>(
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
