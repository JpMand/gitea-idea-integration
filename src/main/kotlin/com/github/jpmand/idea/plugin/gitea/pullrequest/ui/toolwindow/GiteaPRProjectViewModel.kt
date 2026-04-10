package com.github.jpmand.idea.plugin.gitea.pullrequest.ui.toolwindow

import com.github.jpmand.idea.plugin.gitea.GiteaRepositoriesManager
import com.github.jpmand.idea.plugin.gitea.authentication.account.GiteaAccount
import com.github.jpmand.idea.plugin.gitea.authentication.account.GiteaAccountManager
import com.github.jpmand.idea.plugin.gitea.pullrequest.GiteaPRRepositoryConnectionManager
import com.github.jpmand.idea.plugin.gitea.util.GiteaGitRepositoryMapping
import com.intellij.collaboration.async.childScope
import com.intellij.collaboration.async.combineState
import com.intellij.collaboration.async.mapState
import com.intellij.collaboration.util.URIUtil
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow


@Service(Service.Level.PROJECT)
class GiteaPRProjectViewModel(private val project: Project, parentCs: CoroutineScope) {
  private val accountManager: GiteaAccountManager get() = service()
  private val repositoriesManager : GiteaRepositoriesManager get() = project.service()
  private val connectionManager : GiteaPRRepositoryConnectionManager get() = project.service()

  private val cs = parentCs.childScope(this::class)

  val isAvailable: StateFlow<Boolean> = repositoriesManager.knownRepositoriesState.mapState {
    it.isNotEmpty()
  }

  private val _activationRequests = MutableSharedFlow<Unit>(1)

  val activationRequests: SharedFlow<Unit> = _activationRequests.asSharedFlow()

  private val singleRepoAndAccountState : StateFlow<Pair<GiteaGitRepositoryMapping, GiteaAccount>?> =
    combineState(cs, repositoriesManager.knownRepositoriesState, accountManager.accountsState) { repos, accounts ->
      repos.singleOrNull()?.let {repo ->
        accounts.singleOrNull { URIUtil.equalWithoutSchema(it.server.toURI(), repo.repository.serverPath.toURI()) }?.let{
          repo to it
        }
      }
    }

  val selectorVm : GiteaRepositoryAndAccountSelectorViewModel by lazy {

  }
}