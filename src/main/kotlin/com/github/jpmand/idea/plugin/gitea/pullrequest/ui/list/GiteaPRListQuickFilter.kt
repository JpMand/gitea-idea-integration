package com.github.jpmand.idea.plugin.gitea.pullrequest.ui.list

import com.github.jpmand.idea.plugin.gitea.pullrequest.ui.filters.GiteaPRListSearchValue
import com.intellij.collaboration.ui.codereview.list.search.ReviewListQuickFilter

sealed class GiteaPRListQuickFilter(
  override val filter: GiteaPRListSearchValue,
) : ReviewListQuickFilter<GiteaPRListSearchValue> {

    object Open : GiteaPRListQuickFilter(GiteaPRListSearchValue(state = GiteaPRListSearchValue.State.OPEN))
    object Closed : GiteaPRListQuickFilter(GiteaPRListSearchValue(state = GiteaPRListSearchValue.State.CLOSED))
    object All : GiteaPRListQuickFilter(GiteaPRListSearchValue(state = GiteaPRListSearchValue.State.ALL))
}
