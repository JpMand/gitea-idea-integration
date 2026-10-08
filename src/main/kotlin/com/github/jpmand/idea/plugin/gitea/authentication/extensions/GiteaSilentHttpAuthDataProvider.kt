package com.github.jpmand.idea.plugin.gitea.authentication.extensions

import com.github.jpmand.idea.plugin.gitea.api.GiteaApiManager
import com.github.jpmand.idea.plugin.gitea.api.rest.currentUser
import com.github.jpmand.idea.plugin.gitea.authentication.account.GiteaAccount
import com.github.jpmand.idea.plugin.gitea.authentication.account.GiteaAccountManager
import com.github.jpmand.idea.plugin.gitea.authentication.account.GiteaProjectDefaultAccountHolder
import com.intellij.collaboration.auth.AccountManager
import com.intellij.collaboration.auth.DefaultAccountHolder
import com.intellij.diagnostic.rethrowControlFlowException
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.util.AuthData
import git4idea.remote.hosting.http.HostedGitAuthenticationFailureManager
import git4idea.remote.hosting.http.SilentHostedGitHttpAuthDataProviderBase

private val LOG = logger<GiteaSilentHttpAuthDataProvider>()

@Suppress("UnstableApiUsage")
class GiteaSilentHttpAuthDataProvider : SilentHostedGitHttpAuthDataProviderBase<GiteaAccount, String>() {
  override val providerId: String = "Gitea Plugin"

  override val accountManager: AccountManager<GiteaAccount, String>
    get() = service<GiteaAccountManager>()

  override fun getDefaultAccountHolder(project: Project): DefaultAccountHolder<GiteaAccount> {
    return project.service<GiteaProjectDefaultAccountHolder>()
  }

  override fun getAuthFailureManager(project: Project): HostedGitAuthenticationFailureManager<GiteaAccount> {
    return project.service<GiteaGitAuthenticationFailureManager>()
  }

  override suspend fun getAuthData(account: GiteaAccount): AuthData? {
    LOG.debug("Gitea Silent: getting auth data for $account")
    val token = accountManager.findCredentials(account) ?: return null
    return try {
      val login = service<GiteaApiManager>().getClient(account.server, token).currentUser().name
      AuthData(login, token)
    }
    catch (e: Exception) {
      rethrowControlFlowException(e)
      LOG.info("Cannot load details for $account", e)
      null
    }
  }
}
