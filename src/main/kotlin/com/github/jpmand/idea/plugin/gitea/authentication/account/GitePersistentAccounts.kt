package com.github.jpmand.idea.plugin.gitea.authentication.account

import com.intellij.collaboration.auth.ObservableAccountsRepository
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.SettingsCategory
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import kotlinx.coroutines.flow.MutableStateFlow

@State(
    name = "GiteaAccounts",
    storages = [Storage("gitea.xml")],
    reportStatistic = false,
    category = SettingsCategory.TOOLS
)
class GitePersistentAccounts : ObservableAccountsRepository<GiteaAccount>,
    PersistentStateComponent<Array<GiteaAccount>> {

    override var accounts: Set<GiteaAccount>
        get() = accountsFlow.value
        set(value) {
            accountsFlow.value = value
        }

    override val accountsFlow = MutableStateFlow(emptySet<GiteaAccount>())

    override fun getState(): Array<GiteaAccount> = accountsFlow.value.toTypedArray()

    override fun loadState(state: Array<GiteaAccount>) {
        accountsFlow.value = state.toSet()
    }

    override fun noStateLoaded() {
        loadState(emptyArray())
    }

}