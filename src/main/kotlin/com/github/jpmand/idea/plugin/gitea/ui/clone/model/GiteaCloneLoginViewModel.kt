package com.github.jpmand.idea.plugin.gitea.ui.clone.model

import com.github.jpmand.idea.plugin.gitea.api.GiteaServerPath
import com.github.jpmand.idea.plugin.gitea.authentication.account.GiteaAccount
import com.github.jpmand.idea.plugin.gitea.authentication.account.GiteaAccountManager
import com.github.jpmand.idea.plugin.gitea.authentication.ui.GiteaTokenLoginPanelModel
import com.intellij.collaboration.auth.ui.login.LoginModel
import com.intellij.platform.util.coroutines.childScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

internal interface GiteaCloneLoginViewModel : GiteaClonePanelViewModel {
    val accounts : SharedFlow<Set<GiteaAccount>>
    val tokenLoginModel : GiteaTokenLoginPanelModel
}

@Suppress("UnstableApiUsage")
internal class GiteaCloneLoginViewModelImpl(
    parentCs : CoroutineScope,
    private val accountManager: GiteaAccountManager,
) : GiteaCloneLoginViewModel {
    private val cs : CoroutineScope = parentCs.childScope(javaClass.name)

    private var selectedAccount: GiteaAccount? = null
    override val accounts : SharedFlow<Set<GiteaAccount>> = accountManager.accountsState

    override val tokenLoginModel: GiteaTokenLoginPanelModel = GiteaTokenLoginPanelModel(
        requiredUsername = null,
        uniqueAccountPredicate = accountManager::isAccountUnique,
    )

    init {
        cs.launch {
            with(tokenLoginModel) {
                loginState.collect { loginState ->
                    if(loginState is LoginModel.LoginState.Connected) {
                        val storedAccount = selectedAccount ?: GiteaAccount(name = loginState.username, server = getServerPath())
                        updateAccount(storedAccount, token)
                    }
                }
            }
        }
    }

    fun setSelectedAccount(account: GiteaAccount?) {
        selectedAccount = account
        with(tokenLoginModel) {
            requiredUsername = account?.name
            uniqueAccountPredicate = if(account == null) accountManager::isAccountUnique else { _ , _ -> true }
            serverUri = account?.server?.toString() ?: GiteaServerPath.DEFAULT_SERVER.toString()
        }
    }

    private suspend fun updateAccount(account: GiteaAccount, credentials : String) {
        accountManager.updateAccount(account, credentials)
    }
}