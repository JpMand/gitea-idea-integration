package com.github.jpmand.idea.plugin.gitea.pullrequest.service

import com.github.jpmand.idea.plugin.gitea.GiteaRepositoriesManager
import com.github.jpmand.idea.plugin.gitea.api.GiteaApi
import com.github.jpmand.idea.plugin.gitea.api.GiteaApiManager
import com.github.jpmand.idea.plugin.gitea.api.models.GiteaPullRequest
import com.github.jpmand.idea.plugin.gitea.api.rest.models.GiteaStateEnum
import com.github.jpmand.idea.plugin.gitea.api.rest.repoListPullRequests
import com.github.jpmand.idea.plugin.gitea.authentication.account.GiteaAccountManager
import com.github.jpmand.idea.plugin.gitea.util.GiteaGitRepositoryMapping
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn

@Service(Service.Level.PROJECT)
class GiteaPullRequestsProjectService(
    private val project: Project,
    private val cs: CoroutineScope
) {
    private val accountManager: GiteaAccountManager = service()
    private val apiManager: GiteaApiManager = service()
    private val repositoriesManager: GiteaRepositoriesManager by lazy { project.service() }

    /** Incrementing this value triggers a PR list reload. */
    private val _refreshCounter = MutableStateFlow(0)

    val activeRepoMappingState: StateFlow<GiteaGitRepositoryMapping?> by lazy {
        repositoriesManager.knownRepositoriesState
            .combine(accountManager.accountsState) { repos, accounts ->
                // Prefer a mapping whose server has a configured account
                repos.firstOrNull { mapping ->
                    accounts.any { it.server == mapping.repository.serverPath }
                } ?: repos.firstOrNull()
            }
            .stateIn(cs, SharingStarted.Eagerly, null)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val pullRequestsState: StateFlow<Result<List<GiteaPullRequest>>?> by lazy {
        combine(activeRepoMappingState, _refreshCounter) { mapping, _ -> mapping }
            .mapLatest { mapping ->
                if (mapping == null) return@mapLatest Result.success(emptyList())
                runCatching {
                    val api = getApiForMapping(mapping) ?: return@runCatching emptyList()
                    val owner = mapping.repository.repositoryPath.owner
                    val repo = mapping.repository.repositoryPath.repository
                    api.repoListPullRequests(
                        owner = owner,
                        repo = repo,
                        baseBranch = null,
                        state = GiteaStateEnum.OPEN,
                        sort = null,
                        milestone = null,
                        labels = null,
                        poster = null,
                        page = 1,
                        limit = 25
                    ).map { it.toPullRequest() }
                }
            }
            .stateIn(cs, SharingStarted.Eagerly, null)
    }

    /** Triggers a reload of the pull request list. */
    fun refresh() {
        _refreshCounter.value++
    }

    /** Returns a [GiteaApi] for the currently active repository, or null if unavailable. */
    suspend fun getOrLoadApiForActiveRepo(): GiteaApi? {
        val mapping = activeRepoMappingState.value ?: return null
        return getApiForMapping(mapping)
    }

    /** Creates a data loader scoped to the given pull request. */
    fun getDataLoader(owner: String, repo: String, index: Int): GiteaPullRequestDataLoader =
        GiteaPullRequestDataLoader(cs, this, owner, repo, index)

    private suspend fun getApiForMapping(mapping: GiteaGitRepositoryMapping): GiteaApi? {
        val account = accountManager.accountsState.value
            .firstOrNull { it.server == mapping.repository.serverPath } ?: return null
        val token = accountManager.findCredentials(account) ?: return null
        return apiManager.getClient(account.server, token)
    }
}