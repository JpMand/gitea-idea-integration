package com.github.jpmand.idea.plugin.gitea.pullrequest.ui.filters

import com.github.jpmand.idea.plugin.gitea.pullrequest.ui.list.GiteaPRListQuickFilter
import com.github.jpmand.idea.plugin.gitea.util.GiteaBundle
import com.intellij.collaboration.ui.codereview.list.search.ChooserPopupUtil
import com.intellij.collaboration.ui.codereview.list.search.DropDownComponentFactory
import com.intellij.collaboration.ui.codereview.list.search.ReviewListSearchPanelFactory
import com.intellij.collaboration.ui.util.popup.PopupItemPresentation
import kotlinx.coroutines.CoroutineScope
import org.jetbrains.annotations.Nls
import javax.swing.JComponent

@Suppress("UnstableApiUsage")
class GiteaPRListSearchPanelFactory(vm: GiteaPRListSearchPanelViewModel) :
    ReviewListSearchPanelFactory<GiteaPRListSearchValue, GiteaPRListQuickFilter, GiteaPRListSearchPanelViewModel>(vm) {

    override fun getShortText(searchValue: GiteaPRListSearchValue): @Nls String = buildString {
        if (searchValue.searchQuery != null) append("\"${searchValue.searchQuery}\" ")
        append(searchValue.state.displayName)
        searchValue.author?.let { append(" ").append(GiteaBundle.message("pull.request.filter.short.author", it)) }
        searchValue.label?.let { append(" ").append(GiteaBundle.message("pull.request.filter.short.label", it)) }
        searchValue.sort?.let { append(" ").append(GiteaBundle.message("pull.request.filter.short.sort", GiteaBundle.message(it.bundleKey))) }
    }

    override fun createFilters(viewScope: CoroutineScope): List<JComponent> = listOf(
        createStateFilter(viewScope),
        createAuthorFilter(viewScope),
        createLabelFilter(viewScope),
        createSortFilter(viewScope),
    )

    private fun createStateFilter(viewScope: CoroutineScope): JComponent =
        DropDownComponentFactory(vm.stateFilter).create(
            vmScope = viewScope,
            filterName = GiteaBundle.message("pull.request.filter.state"),
            // OPEN is the implicit default (reads back as no selection); offer the other two.
            items = listOf(GiteaPRListSearchValue.State.CLOSED, GiteaPRListSearchValue.State.ALL),
            onSelect = {},
            valuePresenter = { it.displayName },
        )

    private fun createSortFilter(viewScope: CoroutineScope): JComponent =
        DropDownComponentFactory(vm.sortFilter).create(
            vmScope = viewScope,
            filterName = GiteaBundle.message("pull.request.filter.sort"),
            items = GiteaPRListSearchValue.Sort.entries,
            onSelect = {},
            valuePresenter = { GiteaBundle.message(it.bundleKey) },
        )

    private fun createAuthorFilter(viewScope: CoroutineScope): JComponent =
        DropDownComponentFactory(vm.authorFilter).create(
            vmScope = viewScope,
            filterName = GiteaBundle.message("pull.request.filter.author"),
            valuePresenter = { it },
            chooseValue = { point ->
                ChooserPopupUtil.showAsyncChooserPopup(
                    point,
                    vm.authorOptions,
                    { user -> PopupItemPresentation.Simple(user.fullName ?: user.login, null, user.login) },
                )?.login
            },
        )

    private fun createLabelFilter(viewScope: CoroutineScope): JComponent =
        DropDownComponentFactory(vm.labelFilter).create(
            vmScope = viewScope,
            filterName = GiteaBundle.message("pull.request.filter.label"),
            valuePresenter = { it },
            chooseValue = { point ->
                ChooserPopupUtil.showAsyncChooserPopup(
                    point,
                    vm.labelOptions,
                    { label -> PopupItemPresentation.Simple(label.name, null, null) },
                )?.name
            },
        )

    override fun GiteaPRListQuickFilter.getQuickFilterTitle(): @Nls String = when (this) {
        GiteaPRListQuickFilter.Open -> GiteaBundle.message("pull.request.list.filter.open")
        GiteaPRListQuickFilter.Closed -> GiteaBundle.message("pull.request.list.filter.closed")
        GiteaPRListQuickFilter.All -> GiteaBundle.message("pull.request.list.filter.all")
    }
}
