package com.github.jpmand.idea.plugin.gitea.authentication.extensions

import com.github.jpmand.idea.plugin.gitea.api.GiteaApiManager
import com.github.jpmand.idea.plugin.gitea.api.rest.currentUser
import com.github.jpmand.idea.plugin.gitea.authentication.account.GiteaAccount
import com.github.jpmand.idea.plugin.gitea.authentication.account.GiteaAccountManager
import com.github.jpmand.idea.plugin.gitea.authentication.account.GiteaProjectDefaultAccountHolder
import com.intellij.collaboration.auth.AccountManager
import com.intellij.collaboration.auth.DefaultAccountHolder
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.project.Project
import git4idea.remote.hosting.http.HostedGitAuthenticationFailureManager
import git4idea.remote.hosting.http.SilentHostedGitHttpAuthDataProvider

private val LOG = logger<GiteaSilentHttpAuthDataProvider>()

@Suppress("UnstableApiUsage")
class GiteaSilentHttpAuthDataProvider: SilentHostedGitHttpAuthDataProvider<GiteaAccount>() {
  override val providerId: String = "Gitea Plugin"

  override val accountManager: AccountManager<GiteaAccount, String>
    get() = service<GiteaAccountManager>()

  override fun getDefaultAccountHolder(project: Project): DefaultAccountHolder<GiteaAccount> {
    return project.service<GiteaProjectDefaultAccountHolder>()
  }

  override fun getAuthFailureManager(project: Project): HostedGitAuthenticationFailureManager<GiteaAccount> {
    return project.service<GiteaGitAuthenticationFailureManager>()
  }

  override suspend fun getAccountLogin(account: GiteaAccount, token: String): String? {
    LOG.debug("Gitea Silent: getting login for $account")
    return try {
      service<GiteaApiManager>().getClient(account.server, token).currentUser().name
    }
    catch (e: ProcessCanceledException) {
      throw e
    }
    catch (e: Exception) {
      LOG.info("Cannot load details for $account", e)
      null
    }
  }
}