package com.github.jpmand.idea.plugin.gitea.authentication.extensions

import com.github.jpmand.idea.plugin.gitea.api.GiteaServerPath
import com.github.jpmand.idea.plugin.gitea.authentication.GiteLoginUtil.LoginResult
import com.github.jpmand.idea.plugin.gitea.authentication.account.GiteaAccount
import com.github.jpmand.idea.plugin.gitea.authentication.account.GiteaAccountManager
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.asContextElement
import com.intellij.openapi.components.serviceAsync
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.runBlockingMaybeCancellable
import com.intellij.openapi.project.Project
import com.intellij.util.AuthData
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import git4idea.remote.GitHttpAuthDataProvider
import git4idea.remote.hosting.GitHostingUrlUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GiteaHttpAuthDataProvider : GitHttpAuthDataProvider {

  @RequiresBackgroundThread
  override fun getAuthData(project: Project, url: String, login: String): AuthData? =
    runBlockingMaybeCancellable {
      when (val loginResult = performLogin(project, url, login)) {
        is LoginResult.Success -> AuthData(loginResult.account.name, loginResult.token)
        is LoginResult.OtherMethod -> null
        is LoginResult.Failure -> throw ProcessCanceledException()
      }
    }

  @RequiresBackgroundThread
  override fun getAuthData(project: Project, url: String): AuthData? =
    runBlockingMaybeCancellable {
      when (val loginResult = performLogin(project, url)) {
        is LoginResult.Success -> AuthData(loginResult.account.name, loginResult.token)
        is LoginResult.OtherMethod -> null
        is LoginResult.Failure -> throw ProcessCanceledException()
      }
    }

  private suspend fun performLogin(project: Project, gitHostUrl: String, login: String? = null): LoginResult {
    val accountManager = serviceAsync<GiteaAccountManager>()
    val accountsWithToken = accountManager.accountsState.value
      .filter { GitHostingUrlUtil.matchHost(it.server.toURI(), gitHostUrl) }
      .associateWith { accountManager.findCredentials(it) }

    val loginResult: LoginResult = withContext(Dispatchers.EDT + ModalityState.any().asContextElement()) {
      when (accountsWithToken.size) {
        0 -> accountManager.createAccount(project, gitHostUrl, login)
        1 -> accountManager.reLogInWithAccount(project, accountsWithToken.keys.single(), login)
        else -> accountManager.selectAccountAndLogin(project, accountsWithToken, gitHostUrl, login)
      }
    }

    if (loginResult is LoginResult.Success) {
      accountManager.updateAccount(loginResult.account, loginResult.token)
    }

    return loginResult
  }

  private suspend fun GiteaAccountManager.createAccount(
    project: Project,
    gitHostUrl: String,
    login: String? = null
  ): LoginResult {
    val server = GitHostingUrlUtil.getUriFromRemoteUrl(gitHostUrl)?.let {
      GiteaServerPath.from(it.toString())
    } ?: return LoginResult.OtherMethod

    return withContext(Dispatchers.EDT + ModalityState.any().asContextElement()) {
      TODO("Login via token : LoginResult")
    }
  }

  private suspend fun GiteaAccountManager.reLogInWithAccount(
    project: Project,
    account: GiteaAccount,
    login: String? = null
  ): LoginResult = withContext(Dispatchers.EDT + ModalityState.any().asContextElement()) {
    TODO("update token : LoginResult")
  }

  private suspend fun GiteaAccountManager.selectAccountAndLogin(
    project: Project,
    accountsWithToken: Map<GiteaAccount, String?>,
    url: String,
    login: String? = null
  ): LoginResult = withContext(Dispatchers.EDT + ModalityState.any().asContextElement()) {
    val description = "account.choose.git.description: ${url}"
    val account = TODO("choose account : LoginResult") ?: return@withContext LoginResult.Failure
    val token = accountsWithToken[account]
    if (token == null) {
      TODO("update token")
    } else {
      LoginResult.Success(account, token)
    }
  }
}