package com.github.jpmand.idea.plugin.gitea.pullrequest.ui

import com.github.jpmand.idea.plugin.gitea.api.models.GiteaPullRequest
import com.intellij.openapi.actionSystem.DataKey

object GiteaPRActionKeys {
    val SELECTED_PULL_REQUEST: DataKey<GiteaPullRequest> = DataKey.create("Gitea.PullRequest.Selected")
}
