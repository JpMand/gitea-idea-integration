package com.github.jpmand.idea.plugin.gitea.pullrequest.ui.list

import com.intellij.collaboration.ui.codereview.list.search.ReviewListSearchValue

data class GiteaPRListSearchValue(
    override val searchQuery: String? = null,
    val state: State = State.OPEN,
) : ReviewListSearchValue {

    override val filterCount: Int
        get() {
            var count = if (searchQuery != null) 1 else 0
            if (state != State.OPEN) count++
            return count
        }

    companion object {
        val EMPTY = GiteaPRListSearchValue()
        val DEFAULT = GiteaPRListSearchValue(state = State.OPEN)
    }

    enum class State(val apiValue: String, val displayName: String) {
        OPEN("open", "Open"),
        CLOSED("closed", "Closed"),
        ALL("all", "All"),
    }
}
