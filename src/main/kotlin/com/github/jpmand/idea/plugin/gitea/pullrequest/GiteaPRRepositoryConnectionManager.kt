package com.github.jpmand.idea.plugin.gitea.pullrequest

import com.github.jpmand.idea.plugin.gitea.GiteaRepositoriesManager
import com.github.jpmand.idea.plugin.gitea.api.GiteaApiManager
import com.github.jpmand.idea.plugin.gitea.api.GiteaRepositoryConnection
import com.github.jpmand.idea.plugin.gitea.authentication.account.GiteaAccount
import com.github.jpmand.idea.plugin.gitea.authentication.account.GiteaAccountManager
import com.github.jpmand.idea.plugin.gitea.util.GiteaGitRepositoryMapping
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.platform.util.coroutines.childScope
import git4idea.remote.hosting.SingleHostedGitRepositoryConnectionManager
import git4idea.remote.hosting.SingleHostedGitRepositoryConnectionManagerImpl
import git4idea.remote.hosting.ValidatingHostedGitRepositoryConnectionFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@Service(Service.Level.PROJECT)
internal class GiteaPRRepositoryConnectionManager (project: Project, parentCs: CoroutineScope) : SingleHostedGitRepositoryConnectionManager<GiteaGitRepositoryMapping, GiteaAccount, GiteaRepositoryConnection> {

  private val cs = parentCs.childScope(javaClass.name)

  private val repositoriesManager = project.service<GiteaRepositoriesManager>()
  private val accountManager = service<GiteaAccountManager>()

  private val connectionFactory =
    ValidatingHostedGitRepositoryConnectionFactory({repositoriesManager}, {accountManager}) { repo, account, tokenState ->
      createConnection(this, project, tokenState, repo, account)
    }

  private val delegate = SingleHostedGitRepositoryConnectionManagerImpl(parentCs, connectionFactory)

  override val connectionState: StateFlow<GiteaRepositoryConnection?>
    get() = delegate.connectionState

  init{
    cs.launch {
      accountManager.accountsState.collect {
        val currentAccount = connectionState.value?.account
        if (currentAccount != null && !it.contains(currentAccount)) {
          closeConnection()
        }
      }
    }
  }

  private suspend fun createConnection(
    connectionScope: CoroutineScope,
    project: Project,
    tokenState: StateFlow<String>,
    repo: GiteaGitRepositoryMapping,
    account: GiteaAccount
  ) : GiteaRepositoryConnection {
    val apiClient = service<GiteaApiManager>().getClient(account.server) {tokenState.value}
    return GiteaRepositoryConnection( project, connectionScope, repo, account, apiClient, tokenState)
  }

  override suspend fun openConnection(repo: GiteaGitRepositoryMapping, account: GiteaAccount): GiteaRepositoryConnection? =
    delegate.openConnection(repo, account)

  override suspend fun closeConnection() = delegate.closeConnection()
}