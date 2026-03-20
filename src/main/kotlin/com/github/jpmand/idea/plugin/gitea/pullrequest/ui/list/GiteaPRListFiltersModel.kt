package com.github.jpmand.idea.plugin.gitea.pullrequest.ui.list

import com.github.jpmand.idea.plugin.gitea.api.rest.models.GiteaStateEnum
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Holds the current filter values for the pull-request list.
 * Each field is a [MutableStateFlow] so changes can be observed reactively.
 */
class GiteaPRListFiltersModel {
    val state = MutableStateFlow(GiteaStateEnum.OPEN)
    val author = MutableStateFlow<String?>(null)
    val label = MutableStateFlow<String?>(null)
    val assignee = MutableStateFlow<String?>(null)
}
