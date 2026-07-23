package com.github.jpmand.idea.plugin.gitea.ui.clone

import com.github.jpmand.idea.plugin.gitea.api.rest.models.GiteaRepositoryDTO
import com.github.jpmand.idea.plugin.gitea.authentication.account.GiteaAccount
import com.intellij.openapi.util.NlsSafe

internal sealed interface GiteaCloneListItem {
    val account : GiteaAccount

    data class Repository(
        override val account: GiteaAccount,
        val project : GiteaRepositoryDTO
    ) : GiteaCloneListItem

    data class Error(
        override val account: GiteaAccount,
        val error : GiteaCloneException
    ): GiteaCloneListItem
}

internal fun GiteaCloneListItem.Repository.presentation() : @NlsSafe String = project.fullName