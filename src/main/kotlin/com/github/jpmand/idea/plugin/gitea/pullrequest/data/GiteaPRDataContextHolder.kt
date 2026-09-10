package com.github.jpmand.idea.plugin.gitea.pullrequest.data

import com.github.jpmand.idea.plugin.gitea.GiteaRepositoriesManager
import com.github.jpmand.idea.plugin.gitea.api.GiteaApiManager
import com.github.jpmand.idea.plugin.gitea.authentication.account.GiteaAccount
import com.github.jpmand.idea.plugin.gitea.authentication.account.GiteaAccountManager
import com.github.jpmand.idea.plugin.gitea.pullrequest.GiteaPullRequestsSettings
import com.github.jpmand.idea.plugin.gitea.util.GiteaGitRepositoryMapping
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * Project service that tracks the active [GiteaPRDataContext].
 *
 * Reacts to changes in known Git repositories and authenticated accounts to produce
 * a [StateFlow] that emits the current context (or null when none can be resolved).
 *
 * When several `(repository, account)` pairs resolve, the one recorded in
 * [GiteaPullRequestsSettings.selectedUrlAndAccountId] wins; otherwise the first pair that has a
 * stored token is used. Server matching is protocol-insensitive.
 */
@Service(Service.Level.PROJECT)
class GiteaPRDataContextHolder(
    private val project: Project,
    cs: CoroutineScope,
) {
    private val _context = MutableStateFlow<GiteaPRDataContext?>(null)
    val context: StateFlow<GiteaPRDataContext?> = _context.asStateFlow()

    init {
        cs.launch {
            combine(
                project.service<GiteaRepositoriesManager>().knownRepositoriesState,
                service<GiteaAccountManager>().accountsState,
            ) { repos, accounts -> repos to accounts }
                .collectLatest { (repos, accounts) ->
                    _context.value = buildContext(repos, accounts)
                }
        }
    }

    private suspend fun buildContext(
        repos: Set<GiteaGitRepositoryMapping>,
        accounts: Set<GiteaAccount>,
    ): GiteaPRDataContext? {
        val accountManager = service<GiteaAccountManager>()

        // Every (repo, account) whose servers match and whose account has a stored token.
        val candidates = buildList {
            for (mapping in repos) for (account in accounts) {
                if (!account.server.equals(mapping.repository.serverPath, ignoreProtocol = true)) continue
                val token = accountManager.findCredentials(account) ?: continue
                add(Triple(mapping, account, token))
            }
        }
        if (candidates.isEmpty()) return null

        val preferred = project.service<GiteaPullRequestsSettings>().selectedUrlAndAccountId
        val (mapping, account, token) = preferred
            ?.let { (url, accountId) ->
                candidates.firstOrNull { (m, a, _) ->
                    a.id == accountId && m.repository.getWebURI().toString() == url
                }
            }
            ?: candidates.first()

        return GiteaPRDataContext(account, mapping.repository, service<GiteaApiManager>().getClient(account.server, token))
    }
}
