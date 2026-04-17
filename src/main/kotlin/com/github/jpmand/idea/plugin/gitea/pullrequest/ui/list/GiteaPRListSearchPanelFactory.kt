package com.github.jpmand.idea.plugin.gitea.pullrequest.ui.list

import com.github.jpmand.idea.plugin.gitea.util.GiteaBundle
import com.intellij.collaboration.ui.codereview.list.search.ReviewListSearchPanelFactory
import kotlinx.coroutines.CoroutineScope
import org.jetbrains.annotations.Nls
import javax.swing.JComponent

@Suppress("UnstableApiUsage")
class GiteaPRListSearchPanelFactory(vm: GiteaPRListSearchPanelViewModel) :
    ReviewListSearchPanelFactory<GiteaPRListSearchValue, GiteaPRListQuickFilter, GiteaPRListSearchPanelViewModel>(vm) {

    override fun getShortText(searchValue: GiteaPRListSearchValue): @Nls String = buildString {
        if (searchValue.searchQuery != null) append("\"${searchValue.searchQuery}\" ")
        append(searchValue.state.displayName)
    }

    override fun createFilters(viewScope: CoroutineScope): List<JComponent> = emptyList()

    override fun GiteaPRListQuickFilter.getQuickFilterTitle(): @Nls String = when (this) {
        GiteaPRListQuickFilter.Open -> GiteaBundle.message("pull.request.list.filter.open")
        GiteaPRListQuickFilter.Closed -> GiteaBundle.message("pull.request.list.filter.closed")
        GiteaPRListQuickFilter.All -> GiteaBundle.message("pull.request.list.filter.all")
    }
}
