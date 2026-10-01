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
      mutableSetOf<GiteaServerPath>() + accounts.map { it.server }
    }.distinctUntilChanged()

    val discoveredServersFlow = gitRemotesFlow.discoverServers(accountsServersFlow) { remote ->
      // Heuristic: treat the remote's host as a Gitea server. Prefer the remote URL's own scheme
      // when it's explicitly http/https — an http-only self-hosted instance must not be guessed
      // at as https — and fall back to https only when the remote gives no such signal (e.g. an
      // ssh/scp-style git remote, which says nothing about the web-facing scheme the same host
      // serves Gitea's own UI/API over). Likewise the port: an http(s) remote's port is the web
      // port (`http://host:3000/...`), while an ssh remote's is the SSH port, so it's only kept
      // for the former. A server hosted on a sub-path is only discovered once the user
      // configures an account for it (accountsServersFlow).
      val uri = GitHostingUrlUtil.getUriFromRemoteUrl(remote.url)
      val webScheme = uri?.scheme?.takeIf { it == "http" || it == "https" }
      val port = if (webScheme != null && uri.port > 0) ":${uri.port}" else ""
      uri?.host?.let { host -> runCatching { GiteaServerPath.from("${webScheme ?: "https"}://$host$port") }.getOrNull() }
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