package com.github.jpmand.idea.plugin.gitea.pullrequest.data

import com.github.jpmand.idea.plugin.gitea.GiteaRepositoriesManager
import com.github.jpmand.idea.plugin.gitea.api.GiteaApiManager
import com.github.jpmand.idea.plugin.gitea.authentication.account.GiteaAccount
import com.github.jpmand.idea.plugin.gitea.authentication.account.GiteaAccountManager
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
 * The first repository whose server matches any known account is used.
 */
@Suppress("UnstableApiUsage")
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
        val mapping = repos.firstOrNull() ?: return null
        val account = accounts.firstOrNull { it.server == mapping.repository.serverPath } ?: return null
        val token = service<GiteaAccountManager>().findCredentials(account) ?: return null
        val api = service<GiteaApiManager>().getClient(account.server, token)
        return GiteaPRDataContext(account, mapping.repository, api)
    }
}
