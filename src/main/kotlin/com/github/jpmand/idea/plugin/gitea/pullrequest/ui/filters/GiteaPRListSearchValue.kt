package com.github.jpmand.idea.plugin.gitea.pullrequest.ui.filters

import com.github.jpmand.idea.plugin.gitea.api.rest.pr.GiteaPullRequestSortEnum
import com.intellij.collaboration.ui.codereview.list.search.ReviewListSearchValue

data class GiteaPRListSearchValue(
    override val searchQuery: String? = null,
    val state: State = State.OPEN,
    /** Label name (single-select). */
    val label: String? = null,
    /** Author login — passed to the API `poster` filter. */
    val author: String? = null,
    /** null == Gitea's default order ("newest"). */
    val sort: Sort? = null,
) : ReviewListSearchValue {

    override val filterCount: Int
        get() = listOfNotNull(
            searchQuery,
            state.takeIf { it != State.OPEN },
            label,
            author,
            sort,
        ).size

    companion object {
        val EMPTY = GiteaPRListSearchValue()
        val DEFAULT = GiteaPRListSearchValue(state = State.OPEN)
    }

    enum class State(val apiValue: String, val displayName: String) {
        OPEN("open", "Open"),
        CLOSED("closed", "Closed"),
        ALL("all", "All"),
    }

    /**
     * Every sort order Gitea's `/pulls` endpoint accepts, plus [NEWEST] for the implicit default
     * (no `sort` query param).
     */
    enum class Sort(val api: GiteaPullRequestSortEnum?, val bundleKey: String) {
        NEWEST(null, "pull.request.filter.sort.newest"),
        OLDEST(GiteaPullRequestSortEnum.OLDEST, "pull.request.filter.sort.oldest"),
        RECENTLY_UPDATED(GiteaPullRequestSortEnum.RECENTUPDATE, "pull.request.filter.sort.recentupdate"),
        LEAST_RECENTLY_UPDATED(GiteaPullRequestSortEnum.LEASTUPDATE, "pull.request.filter.sort.leastupdate"),
        RECENTLY_CLOSED(GiteaPullRequestSortEnum.RECENTCLOSE, "pull.request.filter.sort.recentclose"),
        MOST_COMMENTED(GiteaPullRequestSortEnum.MOSTCOMMENT, "pull.request.filter.sort.mostcomment"),
        LEAST_COMMENTED(GiteaPullRequestSortEnum.LEASTCOMMENT, "pull.request.filter.sort.leastcomment"),
        PRIORITY(GiteaPullRequestSortEnum.PRIORITY, "pull.request.filter.sort.priority"),
    }
}
