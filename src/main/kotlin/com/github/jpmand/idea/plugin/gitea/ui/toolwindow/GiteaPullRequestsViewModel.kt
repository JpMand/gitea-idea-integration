package com.github.jpmand.idea.plugin.gitea.ui.toolwindow

import com.github.jpmand.idea.plugin.gitea.GiteaRepositoriesManager
import com.github.jpmand.idea.plugin.gitea.api.GiteaApiManager
import com.github.jpmand.idea.plugin.gitea.api.models.GiteaPullRequest
import com.github.jpmand.idea.plugin.gitea.api.rest.repoListPullRequests
import com.github.jpmand.idea.plugin.gitea.authentication.account.GiteaAccountManager
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for managing Pull Requests state in the tool window
 */
class GiteaPullRequestsViewModel(
  private val project: Project,
  private val cs: CoroutineScope
) {
  private val _state = MutableStateFlow<PRState>(PRState.Empty)
  val state: StateFlow<PRState> = _state.asStateFlow()

  private val accountManager = service<GiteaAccountManager>()
  private val apiManager = service<GiteaApiManager>()
  private val repositoriesManager = project.service<GiteaRepositoriesManager>()

  sealed class PRState {
    object Empty : PRState()
    object Loading : PRState()
    data class Success(val pullRequests: List<GiteaPullRequest>) : PRState()
    data class Error(val message: String) : PRState()
  }

  fun loadPullRequests() {
    cs.launch {
      try {
        _state.value = PRState.Loading

        // Get the first known repository mapping
        val mapping = repositoriesManager.knownRepositoriesState.value.firstOrNull()
        if (mapping == null) {
          _state.value = PRState.Error("No Gitea repository found in the current project")
          return@launch
        }

        val repositoryCoordinates = mapping.repository
        val owner = repositoryCoordinates.repositoryPath.owner
        val repo = repositoryCoordinates.repositoryPath.repository

        // Get the first available account
        val account = accountManager.accountsState.value.firstOrNull()
        if (account == null) {
          _state.value = PRState.Error("No Gitea account configured. Please add an account in Settings.")
          return@launch
        }

        // Get API client
        val api = apiManager.getClient(account.server, account)

        // Fetch pull requests
        val prDTOs = api.repoListPullRequests(
          owner = owner,
          repo = repo,
          baseBranch = null,
          state = null,
          sort = null,
          milestone = null,
          labels = null,
          poster = null,
          page = null,
          limit = null
        )

        // Convert to domain models
        val pullRequests = prDTOs.map { it.toPullRequest() }
        _state.value = PRState.Success(pullRequests)

      } catch (e: Exception) {
        LOG.warn("Failed to load pull requests", e)
        _state.value = PRState.Error("Failed to load pull requests: ${e.message}")
      }
    }
  }

  companion object {
    private val LOG = logger<GiteaPullRequestsViewModel>()
  }
}
