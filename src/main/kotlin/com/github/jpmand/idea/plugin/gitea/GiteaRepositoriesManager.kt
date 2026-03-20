package com.github.jpmand.idea.plugin.gitea

import com.github.jpmand.idea.plugin.gitea.api.GiteaServerPath
import com.github.jpmand.idea.plugin.gitea.authentication.account.GiteaAccountManager
import com.github.jpmand.idea.plugin.gitea.util.GiteaGitRepositoryMapping
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import git4idea.remote.hosting.GitHostingUrlUtil
import git4idea.remote.hosting.HostedGitRepositoriesManager
import git4idea.remote.hosting.discoverServers
import git4idea.remote.hosting.gitRemotesFlow
import git4idea.remote.hosting.mapToServers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.runningFold
import kotlinx.coroutines.flow.stateIn

@Suppress("UnstableApiUsage")
interface GiteaRepositoriesManager : HostedGitRepositoriesManager<GiteaGitRepositoryMapping>

internal class GiteaRepositoriesManagerImpl(project: Project, cs: CoroutineScope) : GiteaRepositoriesManager {

  override val knownRepositoriesState: StateFlow<Set<GiteaGitRepositoryMapping>> by lazy {
    val gitRemotesFlow = gitRemotesFlow(project).distinctUntilChanged()

    val accountsServersFlow = service<GiteaAccountManager>().accountsState.map { accounts ->
      mutableSetOf(GiteaServerPath.DEFAULT_SERVER) + accounts.map { it.server }
    }.distinctUntilChanged()

    val discoveredServersFlow = gitRemotesFlow.discoverServers(accountsServersFlow) { remote ->
      @Suppress("UnstableApiUsage")
      GitHostingUrlUtil.findServerAt(LOG, remote) {
        GiteaServerPath.from(it.toString())
      }
    }.runningFold(emptySet<GiteaServerPath>()) { acc, value ->
      acc + value
    }.distinctUntilChanged()

    val serversFlow = accountsServersFlow.combine(discoveredServersFlow) { servers1, servers2 ->
      servers1 + servers2
    }
    @Suppress("UnstableApiUsage")
    val knownRepositoriesFlow = gitRemotesFlow.mapToServers(serversFlow) { server, remote ->
      GiteaGitRepositoryMapping.create(server, remote)
    }.onEach {
      LOG.debug("New list of known repositories: $it")
    }

    knownRepositoriesFlow.stateIn(cs, SharingStarted.Eagerly, emptySet())
  }

  companion object {
    private val LOG = logger<GiteaRepositoriesManager>()
  }
}