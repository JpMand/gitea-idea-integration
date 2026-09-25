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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

/**
 * Project service that tracks the active [GiteaPRDataContext].
 *
 * Reacts to changes in known Git repositories, accounts and their tokens to produce
 * a [StateFlow] that emits the current context (or null when none can be resolved).
 *
 * When several `(repository, account)` pairs resolve, the one recorded in
 * [GiteaPullRequestsSettings.selectedUrlAndAccountId] wins; otherwise the first pair that has a
 * stored token is used. Server matching is protocol-insensitive.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Service(Service.Level.PROJECT)
class GiteaPRDataContextHolder(
    private val project: Project,
    cs: CoroutineScope,
) {
    private val _context = MutableStateFlow<GiteaPRDataContext?>(null)
    val context: StateFlow<GiteaPRDataContext?> = _context.asStateFlow()

    init {
        val accountManager = service<GiteaAccountManager>()
        // An account's token can come and go without the account list changing — e.g. logging back
        // in to an account whose token is missing — and only accounts with a token are usable.
        // getCredentialsFlow only emits on login/logout events, never the current token, so any
        // event re-resolves (merge, not combine: combine would wait for every account to have one)
        // and onStart makes the first resolution happen without one.
        val credentialsChanges = accountManager.accountsState.flatMapLatest { accounts ->
            accounts.map { accountManager.getCredentialsFlow(it) }.merge().map { }.onStart { emit(Unit) }
        }
        cs.launch {
            combine(
                project.service<GiteaRepositoriesManager>().knownRepositoriesState,
                accountManager.accountsState,
                credentialsChanges,
            ) { repos, accounts, _ -> repos to accounts }
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
