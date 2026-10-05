package com.github.jpmand.idea.plugin.gitea

import com.github.jpmand.idea.plugin.gitea.api.GiteaServerPath
import com.github.jpmand.idea.plugin.gitea.authentication.account.GiteaAccountManager
import com.github.jpmand.idea.plugin.gitea.util.GiteaGitRepositoryMapping
import com.intellij.openapi.components.service
import com.intellij.openapi.components.serviceAsync
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
      mutableSetOf<GiteaServerPath>() + accounts.map { it.server }
    }.distinctUntilChanged()

    // A remote not on an account's server is used only once its server proves to be a supported
    // Gitea (GiteaServersManager.isSupportedGiteaServer), so that a project's GitHub or Forgejo
    // remote never gets a Gitea tool window or "Open in Browser" entry.
    val discoveredServersFlow = gitRemotesFlow.discoverServers(accountsServersFlow) { remote ->
      val candidate = giteaServerCandidate(remote.url) ?: return@discoverServers null
      candidate.takeIf { serviceAsync<GiteaServersManager>().isSupportedGiteaServer(it) }
    }.onEach { LOG.trace("Servers discovered from git remotes: $it") }.runningFold(emptySet<GiteaServerPath>()) { acc, value ->
      acc + value
    }.distinctUntilChanged()

    val serversFlow = accountsServersFlow.combine(discoveredServersFlow) { servers1, servers2 ->
      servers1 + servers2
    }
    @Suppress("UnstableApiUsage")
    val knownRepositoriesFlow = gitRemotesFlow.mapToServers(serversFlow) { server, remote ->
      GiteaGitRepositoryMapping.create(server, remote)
    }.onEach {
      // Repository coordinates, not the mappings: a remote URL can carry credentials.
      LOG.debug("Known repositories: ${it.map { mapping -> "${mapping.repository} (${mapping.remote.remote.name})" }}")
    }

    knownRepositoriesFlow.stateIn(cs, SharingStarted.Eagerly, emptySet())
  }

  companion object {
    private val LOG = logger<GiteaRepositoriesManager>()
  }
}

/** Hosts known not to run Gitea: their remotes are never probed. */
private val NON_GITEA_HOSTS = setOf("github.com", "gitlab.com", "bitbucket.org")

/**
 * The Gitea server a git remote would be on, to probe before treating it as one (null when the
 * remote can't be on a Gitea server).
 *
 * An http(s) remote names the web server itself: its scheme, host and port, plus whatever path
 * precedes `owner/repo` — a server on a sub-path (`https://host/gitea/owner/repo.git`). An ssh/scp
 * remote says nothing about the web side (its port is the SSH one), so `https://host` is assumed.
 */
internal fun giteaServerCandidate(remoteUrl: String): GiteaServerPath? {
  val uri = GitHostingUrlUtil.getUriFromRemoteUrl(remoteUrl) ?: return null
  val host = uri.host?.lowercase() ?: return null
  if (host in NON_GITEA_HOSTS) return null
  val scheme = uri.scheme?.lowercase()
  if (scheme != "http" && scheme != "https") return GiteaServerPath.fromOrNull("https://$host")

  val segments = uri.path.orEmpty().removeSuffix("/").removeSuffix(".git").split('/').filter { it.isNotEmpty() }
  if (segments.size < 2) return null
  val port = if (uri.port > 0) ":${uri.port}" else ""
  val subPath = segments.dropLast(2).joinToString("") { "/$it" }
  return GiteaServerPath.fromOrNull("$scheme://$host$port$subPath")
}
