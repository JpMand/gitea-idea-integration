package com.github.jpmand.idea.plugin.gitea.pullrequest.data

import com.github.jpmand.idea.plugin.gitea.GiteaRepositoriesManager
import com.github.jpmand.idea.plugin.gitea.api.GiteaApiManager
import com.github.jpmand.idea.plugin.gitea.api.GiteaRepositoryCoordinates
import com.github.jpmand.idea.plugin.gitea.api.GiteaServerPath
import com.github.jpmand.idea.plugin.gitea.authentication.account.GiteaAccount
import com.github.jpmand.idea.plugin.gitea.authentication.account.GiteaAccountManager
import com.github.jpmand.idea.plugin.gitea.authentication.account.GiteaProjectDefaultAccountHolder
import com.github.jpmand.idea.plugin.gitea.pullrequest.GiteaPullRequestsSettings
import com.github.jpmand.idea.plugin.gitea.util.GiteaGitRepositoryMapping
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CancellationException
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
import com.intellij.openapi.diagnostic.logger

private val LOG = logger<GiteaPRDataContextHolder>()

/**
 * Project service that tracks the active [GiteaPRDataContext].
 *
 * Re-resolves whenever the known Git repositories, the account list, an account's token or the
 * project's default account changes, and emits the current context (or null when none can be
 * resolved). A re-resolution that picks the same account, repository and token keeps the current
 * context, so only a real change reaches the tool window.
 *
 * Which `(repository, account)` pair is used is decided by [preferredCandidate]. Servers are
 * matched with [com.github.jpmand.idea.plugin.gitea.api.GiteaServerPath.equals] ignoring the
 * protocol, and the context's repository is expressed on the account's configured server.
 *
 * Without such a pair, a known repository is read anonymously ([anonymousCandidate]): every known
 * repository is on a Gitea server, either an account's or one detected from the git remote.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Service(Service.Level.PROJECT)
class GiteaPRDataContextHolder(
    private val project: Project,
    cs: CoroutineScope,
) {
    private val _context = MutableStateFlow<GiteaPRDataContext?>(null)
    val context: StateFlow<GiteaPRDataContext?> = _context.asStateFlow()

    /** The token behind [_context], to tell a token change from a plain re-resolution. */
    private var contextToken: String? = null

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
                project.service<GiteaProjectDefaultAccountHolder>().accountState,
                credentialsChanges,
            ) { repos, accounts, defaultAccount, _ -> Triple(repos, accounts, defaultAccount) }
                .collectLatest { (repos, accounts, defaultAccount) ->
                    // A failure here must not end the collection: the tool window would stay on
                    // "no account" until the project is reopened. The next change retries.
                    try {
                        updateContext(repos, accounts, defaultAccount)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        LOG.warn("Couldn't resolve the Gitea account for the pull requests tool window", e)
                    }
                }
        }
    }

    private suspend fun updateContext(
        repos: Set<GiteaGitRepositoryMapping>,
        accounts: Set<GiteaAccount>,
        defaultAccount: GiteaAccount?,
    ) {
        val accountManager = service<GiteaAccountManager>()

        // Every (repo, account) whose servers match and whose account has a stored token.
        val candidates = buildList {
            for (mapping in repos) for (account in accounts) {
                if (!account.server.equals(mapping.repository.serverPath, ignoreProtocol = true)) continue
                val token = accountManager.findCredentials(account) ?: continue
                add(ContextCandidate(mapping, account, token))
            }
        }
        val chosen = preferredCandidate(
            candidates,
            defaultAccount?.takeIf { it in accounts },
            project.service<GiteaPullRequestsSettings>().selectedUrlAndAccountId,
        ) { it.repository.getWebURI().toString() }
        LOG.debug("Resolving the PR context: ${repos.size} known repositories, ${accounts.size} accounts, ${candidates.size} candidates")
        if (chosen == null) {
            updateAnonymousContext(repos, accounts)
            return
        }

        val (mapping, account, token) = chosen
        // The configured server wins over the one discovered from the remote, which may differ in
        // protocol (and so in the web URLs built from it).
        val repo = GiteaRepositoryCoordinates(account.server, mapping.repository.repositoryPath)
        val current = _context.value
        if (current?.account != null && current.account.id == account.id && current.account.server == account.server &&
            current.repo.getWebURI() == repo.getWebURI() && contextToken == token
        ) {
            LOG.debug("PR context unchanged: ${account.name}@${account.server} on $repo")
            return
        }

        LOG.info("PR context: ${account.name}@${account.server} on $repo" +
            if (current?.account?.id == account.id && current.repo.getWebURI() == repo.getWebURI()) " (token changed)" else "")
        contextToken = token
        _context.value = GiteaPRDataContext(account, repo, service<GiteaApiManager>().getClient(account.server, token))
    }

    /** No account with a token on a project remote's server: reads a known repository without one. */
    private fun updateAnonymousContext(repos: Set<GiteaGitRepositoryMapping>, accounts: Set<GiteaAccount>) {
        val mapping = anonymousCandidate(repos.toList(), accounts) { it.repository.serverPath }
        contextToken = null
        if (mapping == null) {
            if (_context.value != null) {
                LOG.info("No PR context: no Gitea server among the project's git remotes " +
                    "(${repos.size} known repositories, ${accounts.size} accounts)")
            }
            _context.value = null
            return
        }
        // An account's configured server wins here too, as for an account context.
        val server = accounts.firstOrNull { it.server.equals(mapping.repository.serverPath, ignoreProtocol = true) }?.server
            ?: mapping.repository.serverPath
        val repo = GiteaRepositoryCoordinates(server, mapping.repository.repositoryPath)
        val current = _context.value
        if (current != null && current.isAnonymous && current.repo.getWebURI() == repo.getWebURI()) {
            LOG.debug("PR context unchanged: anonymous on $repo")
            return
        }
        LOG.info("PR context: anonymous (read-only) on $repo")
        _context.value = GiteaPRDataContext(null, repo, service<GiteaApiManager>().getUnauthenticatedClient(server))
    }
}

/** A repository mapping with an account on the same server, and that account's stored token. */
internal data class ContextCandidate<M>(val mapping: M, val account: GiteaAccount, val token: String) {
    override fun toString(): String = "ContextCandidate(mapping=$mapping, account=$account)"
}

/**
 * The candidate the pull requests tool window should use: one with the project's [defaultAccount]
 * (as chosen in Settings) first, then the repository and account recorded in [selected], then the
 * first one. [webUrl] gives a mapping's repository web URL, to match against [selected].
 */
internal fun <M> preferredCandidate(
    candidates: List<ContextCandidate<M>>,
    defaultAccount: GiteaAccount?,
    selected: Pair<String, String>?,
    webUrl: (M) -> String,
): ContextCandidate<M>? =
    defaultAccount?.let { default -> candidates.firstOrNull { it.account.id == default.id } }
        ?: selected?.let { (url, accountId) -> candidates.firstOrNull { it.account.id == accountId && webUrl(it.mapping) == url } }
        ?: candidates.firstOrNull()

/**
 * The repository to read anonymously when no account with a token can be used: preferably one on a
 * server that has an account (its token is just missing), otherwise the first.
 */
internal fun <M> anonymousCandidate(mappings: List<M>, accounts: Collection<GiteaAccount>, server: (M) -> GiteaServerPath): M? =
    mappings.firstOrNull { m -> accounts.any { it.server.equals(server(m), ignoreProtocol = true) } } ?: mappings.firstOrNull()
