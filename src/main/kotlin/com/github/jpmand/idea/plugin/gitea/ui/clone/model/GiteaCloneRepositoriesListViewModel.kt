package com.github.jpmand.idea.plugin.gitea.ui.clone.model

import com.github.jpmand.idea.plugin.gitea.api.GiteaApiManager
import com.github.jpmand.idea.plugin.gitea.api.rest.userCurrentListRepos
import com.github.jpmand.idea.plugin.gitea.authentication.account.GiteaAccount
import com.github.jpmand.idea.plugin.gitea.authentication.account.GiteaAccountManager
import com.github.jpmand.idea.plugin.gitea.ui.clone.GiteaCloneException
import com.github.jpmand.idea.plugin.gitea.ui.clone.GiteaCloneListItem
import com.intellij.collaboration.api.HttpStatusErrorException
import com.intellij.collaboration.async.flatMapLatestEach
import com.intellij.collaboration.async.mapStatefulToStateful
import com.intellij.collaboration.async.withInitial
import com.intellij.collaboration.messages.CollaborationToolsBundle
import com.intellij.openapi.components.service
import com.intellij.platform.util.coroutines.childScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch
import java.net.ConnectException
import kotlin.coroutines.cancellation.CancellationException

internal interface GiteaCloneRepositoriesForAccountViewModel {
    val account: GiteaAccount

    val isLoading: StateFlow<Boolean>
    val items: StateFlow<List<GiteaCloneListItem>>

    fun reload()
}

@Suppress("UnstableApiUsage")
private class GiteaCloneRepositoriesForAccountViewModelImpl(
    parentCs: CoroutineScope,
    private val accountManager: GiteaAccountManager,
    override val account: GiteaAccount
) : GiteaCloneRepositoriesForAccountViewModel {
    private val cs = parentCs.childScope(javaClass.name)

    private val reloadSignal = MutableSharedFlow<Unit>(1)
    private val apiManager = service<GiteaApiManager>()

    private val _isLoading: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    override val items: StateFlow<List<GiteaCloneListItem>> =
        reloadSignal.withInitial(Unit).transformLatest { _ ->
            try {
                _isLoading.value = true
                val token = accountManager.findCredentials(account) ?: run {
                    emit(listOf(GiteaCloneListItem.Error(account, GiteaCloneException.MissingAccessToken(account))))
                    return@transformLatest
                }
                val apiClient = apiManager.getClient(account.server, token)

                apiClient.rest.userCurrentListRepos(1, 20)
                    .map { l -> GiteaCloneListItem.Repository(account, l) }
                    .let { emit(it) }
            } catch (e: CancellationException) {
                throw e
            } catch (_: ConnectException) {
                emit(listOf(GiteaCloneListItem.Error(account, GiteaCloneException.ConnectionError(account))))
            } catch (e: Throwable) {
                if (e is HttpStatusErrorException && e.statusCode == 401) {
                    emit(listOf(GiteaCloneListItem.Error(account, GiteaCloneException.RevokedToken(account))))
                } else {
                    val message =
                        e.localizedMessage ?: CollaborationToolsBundle.message("clone.dialog.error.load.repositories")
                    emit(listOf(GiteaCloneListItem.Error(account, GiteaCloneException.Unknown(account, message))))
                }
            } finally {
                _isLoading.value = false
            }
        }
            .flowOn(Dispatchers.IO)
            .stateIn(cs, SharingStarted.Eagerly, listOf())

    init {
        cs.launch {
            accountManager.getCredentialsFlow(account).collectLatest {
                reloadSignal.emit(Unit)
            }
        }
    }

    override fun reload() {
        cs.launch {
            reloadSignal.emit(Unit)
        }
    }
}

internal interface GiteaCloneRepositoriesListViewModel {
    val isLoading: StateFlow<Boolean>
    val allAccounts: StateFlow<List<GiteaAccount>>
    val allItems: StateFlow<List<GiteaCloneListItem>>

    fun reload()
    fun reload(account: GiteaAccount)
}

@Suppress("UnstableApiUsage")
internal class GiteaCloneRepositoriesListViewModelImpl(
    parentCs: CoroutineScope,
    accountManager: GiteaAccountManager,
) : GiteaCloneRepositoriesListViewModel {
    private val cs = parentCs.childScope(javaClass.name)

    private val reloadSignal = MutableSharedFlow<Unit>(1)


    @OptIn(ExperimentalCoroutinesApi::class)
    private val listsPerAccount = reloadSignal.withInitial(Unit).flatMapLatest { _ ->
        accountManager.accountsState.mapStatefulToStateful { account ->
            GiteaCloneRepositoriesForAccountViewModelImpl(this, accountManager, account)
        }
    }.stateIn(cs, SharingStarted.Eagerly, listOf())

    @OptIn(ExperimentalCoroutinesApi::class)
    override val isLoading: StateFlow<Boolean> = listsPerAccount.flatMapLatest { vms ->
        combine(vms.map { model -> model.isLoading }) {
            it.any { it }
        }
    }.stateIn(cs, SharingStarted.Eagerly, false)

    override val allAccounts: StateFlow<List<GiteaAccount>> = listsPerAccount.map { vms -> vms.map { it.account } }
        .stateIn(cs, SharingStarted.Eagerly, listOf())

    override val allItems: StateFlow<List<GiteaCloneListItem>> =
        listsPerAccount.flatMapLatestEach { model -> model.items }
            .map { itemList -> itemList.flatMap { it } }
            .stateIn(cs, SharingStarted.Eagerly, listOf())

    override fun reload() {
        cs.launch {
            reloadSignal.emit(Unit)
        }
    }

    override fun reload(account: GiteaAccount) {
        cs.launch {
            listsPerAccount.value.find { it.account == account }?.reload()
        }
    }

}