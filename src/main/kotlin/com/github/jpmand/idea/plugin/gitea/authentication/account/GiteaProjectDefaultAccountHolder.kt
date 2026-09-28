package com.github.jpmand.idea.plugin.gitea.authentication.account

import com.intellij.collaboration.async.childScope
import com.intellij.collaboration.auth.PersistentDefaultAccountHolder
import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Service(Service.Level.PROJECT)
@State(name = "GiteaDefaultAccount", storages = [Storage(StoragePathMacros.WORKSPACE_FILE)], reportStatistic = false)
class GiteaProjectDefaultAccountHolder(project: Project, parentCs: CoroutineScope) :
  PersistentDefaultAccountHolder<GiteaAccount>(
    project,
    parentCs.childScope(GiteaProjectDefaultAccountHolder::class)
  ) {

  private val _accountState = MutableStateFlow<GiteaAccount?>(null)

  /**
   * [account] as a flow, so the PR tool window can follow the default account chosen in
   * Settings. May still hold an account after it's removed — the base class only drops it on
   * read — so compare it against the current account list.
   */
  val accountState: StateFlow<GiteaAccount?> = _accountState.asStateFlow()

  override var account: GiteaAccount?
    get() = super.account
    set(value) {
      super.account = value
      _accountState.value = super.account
    }

  override fun accountManager() = service<GiteaAccountManager>()
  override fun notifyDefaultAccountMissing() {

  }
}
