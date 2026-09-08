package com.github.jpmand.idea.plugin.gitea

import com.github.jpmand.idea.plugin.gitea.api.GiteaApiManager
import com.github.jpmand.idea.plugin.gitea.api.GiteaRepositoryConnection
import com.github.jpmand.idea.plugin.gitea.api.rest.currentUser
import com.github.jpmand.idea.plugin.gitea.api.rest.getRepository
import com.github.jpmand.idea.plugin.gitea.authentication.account.GiteaAccount
import com.github.jpmand.idea.plugin.gitea.authentication.account.GiteaAccountManager
import com.github.jpmand.idea.plugin.gitea.util.GiteaGitRepositoryMapping
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import git4idea.remote.hosting.SingleHostedGitRepositoryConnectionManager
import git4idea.remote.hosting.SingleHostedGitRepositoryConnectionManagerImpl
import git4idea.remote.hosting.ValidatingHostedGitRepositoryConnectionFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@Service(Service.Level.PROJECT)
@Suppress("UnstableApiUsage")
internal class GiteaRepositoryConnectionManager(private val project: Project, parentCs: CoroutineScope) :
    SingleHostedGitRepositoryConnectionManager<GiteaGitRepositoryMapping, GiteaAccount, GiteaRepositoryConnection>{

    private val repositoriesManager = project.service<GiteaRepositoriesManager>()

    private val accountManager = service<GiteaAccountManager>()

    private val connectionFactory =
        ValidatingHostedGitRepositoryConnectionFactory({ repositoriesManager }, { accountManager }){
            repoMapping, account, tokenState ->
            createConnection(this, tokenState, repoMapping, account)
        }

    private val delegate = SingleHostedGitRepositoryConnectionManagerImpl(parentCs, connectionFactory)

    override val connectionState: StateFlow<GiteaRepositoryConnection?>
        get() = delegate.connectionState

    init {
        parentCs.launch {
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
        tokenState: StateFlow<String>,
        repoMapping : GiteaGitRepositoryMapping,
        account: GiteaAccount,
    ) : GiteaRepositoryConnection {
        val api = service<GiteaApiManager>().getClient(account.server, tokenState.value)
        val currentUser = api.currentUser()
        val repositoryDto = api.getRepository(repoMapping.repository.repositoryPath.owner, repoMapping.repository.repositoryPath.repository)
        return GiteaRepositoryConnection(project, connectionScope, account, currentUser, repositoryDto, repoMapping, api, tokenState)
    }

    override suspend fun openConnection(
        repo: GiteaGitRepositoryMapping,
        account: GiteaAccount
    ): GiteaRepositoryConnection? =
        delegate.openConnection(repo, account)

    override suspend fun closeConnection() {
        delegate.closeConnection()
    }
}