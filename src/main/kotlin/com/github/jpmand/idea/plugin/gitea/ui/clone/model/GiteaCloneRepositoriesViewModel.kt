package com.github.jpmand.idea.plugin.gitea.ui.clone.model

import com.github.jpmand.idea.plugin.gitea.api.GiteaApiManager
import com.github.jpmand.idea.plugin.gitea.authentication.account.GiteaAccountManager
import com.github.jpmand.idea.plugin.gitea.authentication.ui.GiteaAccountsDetailsProvider
import com.github.jpmand.idea.plugin.gitea.ui.GiteaSettings
import com.github.jpmand.idea.plugin.gitea.ui.clone.GiteaCloneListItem
import com.github.jpmand.idea.plugin.gitea.ui.clone.model.GiteaCloneRepositoriesViewModel.SearchModel
import com.intellij.collaboration.async.mapState
import com.intellij.dvcs.ui.CloneDvcsValidationUtils
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.CheckoutProvider
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.platform.util.coroutines.childScope
import git4idea.checkout.GitCheckoutProvider
import git4idea.commands.Git
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.net.URI
import java.net.URL
import java.nio.file.Paths

@Suppress("UnstableApiUsage")
internal interface GiteaCloneRepositoriesViewModel : GiteaClonePanelViewModel {
    val listVm: GiteaCloneRepositoriesListViewModel

    val searchValue: SharedFlow<SearchModel>
    val selectedUrl: SharedFlow<String?>

    val accountDetailsProvider: GiteaAccountsDetailsProvider

    val shallowCloneModel: GiteaShallowCloneModel

    fun selectItem(item: GiteaCloneListItem?)

    fun setSearchValue(value: String)

    fun setDirectoryPath(path: String)

    fun doClone(checkoutListener: CheckoutProvider.Listener)

    sealed interface SearchModel {
        class Url(val url: String) : SearchModel
        object Text : SearchModel
    }
}

@Suppress("UnstableApiUsage")
internal class GiteaCloneRepositoriesViewModelImpl(
    private val project: Project,
    parentCs: CoroutineScope,
    private val accountManager: GiteaAccountManager,
) : GiteaCloneRepositoriesViewModel {
    private val apiManager = service<GiteaApiManager>()

    private val cs = parentCs.childScope(javaClass.name)

    override val listVm: GiteaCloneRepositoriesListViewModel = GiteaCloneRepositoriesListViewModelImpl(cs, accountManager)

    private val selectedItem: MutableStateFlow<GiteaCloneListItem?> = MutableStateFlow(null)

    private val _searchValue: MutableStateFlow<String> = MutableStateFlow("")

    override val searchValue: SharedFlow<SearchModel> =
        _searchValue.mapState(cs) { text ->
            try {

                val uri = URI.create(text);
                when (uri.scheme) {
                    "http", "https" -> {
                        URL(text)
                    }

                    "ssh" -> {
                        if (!Regex("""^ssh://([A-Za-z0-9._-]+@)?[A-Za-z0-9.-]+(:\d+)?/.+$""").matches(text)) throw IllegalArgumentException(
                            "Invalid SSH URL"
                        )
                    }

                    else -> throw IllegalArgumentException("Invalid URL")
                }
                SearchModel.Url(text)
            } catch (e: Exception) {
                SearchModel.Text
            }
        }

    private val _selectedUrl: StateFlow<String?> = combine(searchValue, selectedItem) { searchValue, selectedItem ->
        when {
            searchValue is SearchModel.Url -> searchValue.url
            selectedItem != null && selectedItem is GiteaCloneListItem.Repository ->
                if (GiteaSettings.getInstance().cloneWithSsh) selectedItem.project.sshUrl
                else selectedItem.project.htmlUrl

            else -> null
        }
    }.stateIn(cs, SharingStarted.Eagerly, initialValue = null)

    override val selectedUrl: SharedFlow<String?> = _selectedUrl

    private val directoryPath: MutableStateFlow<String> = MutableStateFlow("")

    override val shallowCloneModel = GiteaShallowCloneModel()

    override val accountDetailsProvider: GiteaAccountsDetailsProvider =
        GiteaAccountsDetailsProvider(cs, accountManager) { account ->
            val token = accountManager.findCredentials(account) ?: return@GiteaAccountsDetailsProvider null
            apiManager.getClient(account.server, token)
        }

    override fun selectItem(item: GiteaCloneListItem?) {
        selectedItem.value = item
    }

    override fun setSearchValue(value: String) {
        _searchValue.value = value
    }

    override fun setDirectoryPath(path: String) {
        directoryPath.value = path
    }

    override fun doClone(checkoutListener: CheckoutProvider.Listener) {
        val url = _selectedUrl.value ?: error("Clone: No repository is selected")
        // Inlined from git4idea's internal GitCloneUtils.clone: resolve the parent directory, then
        // hand off to the public GitCheckoutProvider.clone. The clone dialog has already validated
        // the destination via CloneDvcsValidationUtils (doValidateAll), so a failure here is rare.
        val destination = Paths.get(directoryPath.value).toAbsolutePath()
        val parent = destination.parent ?: return
        if (CloneDvcsValidationUtils.createDestination(parent.toString()) != null) return
        val lfs = LocalFileSystem.getInstance()
        val parentDir = lfs.findFileByNioFile(parent) ?: lfs.refreshAndFindFileByNioFile(parent) ?: return

        GitCheckoutProvider.clone(
            project, Git.getInstance(), checkoutListener, parentDir,
            url, destination.fileName.toString(), parent.toString(),
            shallowCloneModel.toOptions(),
        )
    }
}