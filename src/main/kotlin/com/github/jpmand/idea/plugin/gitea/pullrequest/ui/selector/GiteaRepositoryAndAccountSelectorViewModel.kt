package com.github.jpmand.idea.plugin.gitea.pullrequest.ui.selector

import com.github.jpmand.idea.plugin.gitea.GiteaRepositoriesManager
import com.github.jpmand.idea.plugin.gitea.authentication.account.GiteaAccount
import com.github.jpmand.idea.plugin.gitea.authentication.account.GiteaAccountManager
import com.github.jpmand.idea.plugin.gitea.util.GiteaGitRepositoryMapping
import com.intellij.collaboration.async.combineState
import com.intellij.openapi.project.Project
import git4idea.remote.hosting.ui.RepositoryAndAccountSelectorViewModelBase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class GiteaRepositoryAndAccountSelectorViewModel(
  internal val project: Project,
  private val scope : CoroutineScope,
  repositoryManager: GiteaRepositoriesManager,
  val accountManager: GiteaAccountManager,
  onSelected : suspend (GiteaGitRepositoryMapping, GiteaAccount) -> Unit
) : RepositoryAndAccountSelectorViewModelBase<GiteaGitRepositoryMapping, GiteaAccount>(
  scope,
  repositoryManager,
  accountManager,
  onSelected
){
  val tokenLoginAvailableState : StateFlow<Boolean> =
    combineState(scope, repoSelectionState, accountSelectionState, missingCredentialsState, ::isTokenLoginAvailable)

  private fun isTokenLoginAvailable(repo: GiteaGitRepositoryMapping?, account: GiteaAccount?, tokenMisisng: Boolean?) : Boolean =
    repo != null && (account == null && tokenMisisng == true)

  private val _loginRequestFlow = MutableSharedFlow<TokenLoginRequest>()
  val loginRequestFlow: Flow<TokenLoginRequest> = _loginRequestFlow.asSharedFlow()

  fun requestTokenlogin(forceNewAccount: Boolean, submit: Boolean){
    val repo = repoSelectionState.value ?: return
    val account = if(forceNewAccount) null else accountSelectionState.value

    scope.launch {
      _loginRequestFlow.emit(TokenLoginRequest(repo, account, submit))
    }
  }

  inner class TokenLoginRequest(val repo: GiteaGitRepositoryMapping,
                                val account: GiteaAccount? = null,
                                private val submit: Boolean){
    val accounts : Set<GiteaAccount> get() = accountManager.accountsState.value

    fun login(account: GiteaAccount, token: String) {
      scope.launch {
        accountManager.updateAccount(account, token)
        accountSelectionState.value = account
        if (submit) {
          submitSelection()
        }
      }
    }
  }
}