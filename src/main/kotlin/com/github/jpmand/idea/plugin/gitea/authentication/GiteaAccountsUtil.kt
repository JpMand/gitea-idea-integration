package com.github.jpmand.idea.plugin.gitea.authentication

import com.github.jpmand.idea.plugin.gitea.authentication.account.GiteaAccount
import com.github.jpmand.idea.plugin.gitea.authentication.account.GiteaAccountManager
import com.intellij.openapi.components.service

private val accountManager: GiteaAccountManager
    get() = service()

object GiteaAccountsUtil {
    @JvmStatic
    val accounts: Set<GiteaAccount>
        get() = accountManager.accountsState.value

}