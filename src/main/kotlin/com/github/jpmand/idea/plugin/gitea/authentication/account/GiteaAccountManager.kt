package com.github.jpmand.idea.plugin.gitea.authentication.account

import com.github.jpmand.idea.plugin.gitea.api.GiteaServerPath
import com.intellij.collaboration.auth.AccountManagerBase
import com.intellij.collaboration.auth.AccountsRepository
import com.intellij.collaboration.auth.CredentialsRepository
import com.intellij.collaboration.auth.PasswordSafeCredentialsRepository
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger

@Service
class GiteaAccountManager : AccountManagerBase<GiteaAccount, String>(logger<GiteaAccountManager>()), Disposable {

  override fun accountsRepository(): AccountsRepository<GiteaAccount> = service<GitePersistentAccounts>()

  override fun credentialsRepository(): CredentialsRepository<GiteaAccount, String> =
    PasswordSafeCredentialsRepository("Gitea", PasswordSafeCredentialsRepository.CredentialsMapper.Simple)

  companion object {
    fun createAccount(name: String, server: GiteaServerPath) = GiteaAccount(name, server)
  }

  override fun dispose() = Unit

  fun isAccountUnique(server: GiteaServerPath, accountName: String): Boolean {
    return accountsState.value.none { account: GiteaAccount ->
        account.server.equals(server, false) && account.name == accountName
      }
  }
}