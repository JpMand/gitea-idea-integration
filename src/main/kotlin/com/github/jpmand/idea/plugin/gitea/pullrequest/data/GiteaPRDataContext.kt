package com.github.jpmand.idea.plugin.gitea.pullrequest.data

import com.github.jpmand.idea.plugin.gitea.api.GiteaApi
import com.github.jpmand.idea.plugin.gitea.api.GiteaRepositoryCoordinates
import com.github.jpmand.idea.plugin.gitea.authentication.account.GiteaAccount

/** Holds the resolved context (account + repo coordinates + authenticated API) for PR operations. */
data class GiteaPRDataContext(
    val account: GiteaAccount,
    val repo: GiteaRepositoryCoordinates,
    val api: GiteaApi,
)
