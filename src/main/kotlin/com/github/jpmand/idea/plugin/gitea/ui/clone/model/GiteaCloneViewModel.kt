package com.github.jpmand.idea.plugin.gitea.ui.clone.model

import com.github.jpmand.idea.plugin.gitea.authentication.account.GiteaAccount
import com.github.jpmand.idea.plugin.gitea.authentication.account.GiteaAccountManager
import com.intellij.collaboration.auth.ui.login.LoginModel
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.CheckoutProvider
import com.intellij.platform.util.coroutines.childScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

internal interface GiteaCloneViewModel {
    val panelVm: SharedFlow<GiteaClonePanelViewModel>
    fun switchToLoginPanel(account: GiteaAccount?)
    fun switchToRepositoryList()
    fun doClone(checkoutListener: CheckoutProvider.Listener)
}
@Suppress("UnstableApiUsage")
internal class GiteaCloneViewModelImpl(
    project: Project,
    parentCs: CoroutineScope,
    accountManager: GiteaAccountManager,
) : GiteaCloneViewModel {
    private val cs : CoroutineScope = parentCs.childScope(javaClass.name)

    private val loginVm = GiteaCloneLoginViewModelImpl(cs, accountManager)
    private val repositoriesVm = GiteaCloneRepositoriesViewModelImpl(project, cs, accountManager)

    private val accounts : SharedFlow<Set<GiteaAccount>> = accountManager.accountsState

    private val _panelVm : MutableStateFlow<GiteaClonePanelViewModel> = MutableStateFlow(repositoriesVm)
    override val panelVm: StateFlow<GiteaClonePanelViewModel> = _panelVm.asStateFlow()

    init {
        cs.launch {
            accounts.collectLatest { accounts ->
                if (accounts.isNotEmpty()) switchToRepositoryList() else switchToLoginPanel(null)
            }
        }
        cs.launch {
            loginVm.tokenLoginModel.loginState.collectLatest { loginState ->
                if(loginState is LoginModel.LoginState.Connected) switchToRepositoryList()
            }
        }
    }

    override fun switchToLoginPanel(account: GiteaAccount?) {
        loginVm.setSelectedAccount(account)
        _panelVm.value = loginVm
    }

    override fun switchToRepositoryList() {
        _panelVm.value = repositoriesVm
    }

    override fun doClone(checkoutListener: CheckoutProvider.Listener) {
        repositoriesVm.doClone(checkoutListener)
    }
}