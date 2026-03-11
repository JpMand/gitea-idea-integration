package com.github.jpmand.idea.plugin.gitea.authentication.account

import com.github.jpmand.idea.plugin.gitea.api.GiteaServerPath
import com.github.jpmand.idea.plugin.gitea.util.GiteaUtil.SERVICE_NAME
import com.intellij.collaboration.auth.AccountManager
import com.intellij.collaboration.auth.AccountManagerBase
import com.intellij.collaboration.auth.CredentialsRepository
import com.intellij.collaboration.auth.ObservableAccountsRepository
import com.intellij.collaboration.auth.PasswordSafeCredentialsRepository
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger

interface GiteaAccountManager : AccountManager<GiteaAccount, String> {
  fun isAccountUnique(server: GiteaServerPath, accountName: String): Boolean
}

class PersistentGiteaAccountManager :
  GiteaAccountManager,
  AccountManagerBase<GiteaAccount, String>(logger<GiteaAccountManager>()) {

  override fun accountsRepository(): ObservableAccountsRepository<GiteaAccount> = service<GitePersistentAccounts>()

  override fun credentialsRepository(): CredentialsRepository<GiteaAccount, String> =
    PasswordSafeCredentialsRepository(
      SERVICE_NAME,
      PasswordSafeCredentialsRepository.CredentialsMapper.Simple
    )

  override fun isAccountUnique(server: GiteaServerPath, accountName: String): Boolean {
    return accountsState.value.none { account: GiteaAccount ->
      account.server.equals(server, false) && account.name == accountName
    }
  }
}