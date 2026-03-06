package com.github.jpmand.idea.plugin.gitea.authentication.account

import com.intellij.collaboration.auth.AccountsRepository
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.SettingsCategory
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

@State(name = "GiteaAccounts", storages = [Storage("gitea.xml")], reportStatistic = false, category = SettingsCategory.TOOLS)
class GitePersistentAccounts : AccountsRepository<GiteaAccount>, PersistentStateComponent<GiteaAccount> {

  override var accounts: Set<GiteaAccount>
    get() = TODO("Not yet implemented")
    set(value) {}

  override fun getState(): GiteaAccount? {
    TODO("Not yet implemented")
  }

  override fun loadState(state: GiteaAccount) {
    TODO("Not yet implemented")
  }
}