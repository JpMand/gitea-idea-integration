package com.github.jpmand.idea.plugin.gitea.ui

import com.github.jpmand.idea.plugin.gitea.GiteaServersManager
import com.github.jpmand.idea.plugin.gitea.api.GiteaApiManager
import com.github.jpmand.idea.plugin.gitea.authentication.account.GiteaAccountManager
import com.github.jpmand.idea.plugin.gitea.authentication.account.GiteaProjectDefaultAccountHolder
import com.github.jpmand.idea.plugin.gitea.authentication.ui.GiteaAccountsDetailsProvider
import com.github.jpmand.idea.plugin.gitea.authentication.ui.GiteaAccountsListModel
import com.github.jpmand.idea.plugin.gitea.authentication.ui.GiteaAccountsPanelActionsController
import com.github.jpmand.idea.plugin.gitea.util.GiteaBundle.message
import com.github.jpmand.idea.plugin.gitea.util.GiteaPluginProjectScopeProvider
import com.github.jpmand.idea.plugin.gitea.util.GiteaUtil.SERVICE_DISPLAY_NAME
import com.intellij.collaboration.auth.ui.AccountsPanelFactory
import com.intellij.collaboration.auth.ui.AccountsPanelFactory.Companion.addWarningForMemoryOnlyPasswordSafeAndGet
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.asContextElement
import com.intellij.openapi.components.*
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.*
import com.intellij.collaboration.async.mapState
import com.intellij.collaboration.util.CollectableSerializablePersistentStateComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable
import org.jetbrains.annotations.ApiStatus

@Suppress("UnstableApiUsage")
internal class GiteaSettingsConfigurable internal constructor(private val project: Project) :
  BoundConfigurable(SERVICE_DISPLAY_NAME, "settings.gitea") {
  override fun createPanel(): DialogPanel {
    val scopeProvider = project.service<GiteaPluginProjectScopeProvider>()
    val defaultAccountHolder = project.service<GiteaProjectDefaultAccountHolder>()
    val accountManager = service<GiteaAccountManager>()
    val giteaSettings = GiteaSettings.getInstance()

    val scope = scopeProvider.childScope(
      javaClass.name, disposable!!,
      Dispatchers.EDT + ModalityState.any().asContextElement()
    )

    val accountsModel = GiteaAccountsListModel()
    val detailsProvider = GiteaAccountsDetailsProvider(scope, accountsModel) { account ->
      accountsModel.newCredentials.getOrElse(account) {
        accountManager.findCredentials(account)
      }?.let {
        service<GiteaApiManager>().getClient(account.server, it)
      }
    }
    val actionsController = GiteaAccountsPanelActionsController(project, accountsModel)
    val accountsPanelFactory = AccountsPanelFactory(scope, accountManager, defaultAccountHolder, accountsModel)

    return panel {
      row {
        accountsPanelFactory.accountsPanelCell(this, detailsProvider, actionsController)
          .align(Align.FILL)
      }.resizableRow()

      // The label goes in the form's label column, so it lines up like other settings pages.
      row(message("settings.connection.timeout")) {
        intTextField(range = 0..60)
          .columns(2)
          .bindIntText({ giteaSettings.connectionTimeout / 1000 }, { giteaSettings.connectionTimeout = it * 1000 })
          .gap(RightGap.SMALL)
        @Suppress("DialogTitleCapitalization")
        label(message("settings.connection.timeout.seconds"))
          .gap(RightGap.COLUMNS)
      }

      row {
        checkBox(message("settings.clone.with.ssh"))
          .bindSelected(
            { giteaSettings.cloneWithSsh },
            { giteaSettings.cloneWithSsh = it })
      }

      row {
        checkBox(message("settings.reviewer.list.all.users"))
          .bindSelected(
            { giteaSettings.allUsersArePotentialReviewers },
            { giteaSettings.allUsersArePotentialReviewers = it })
      }
      row {
        checkBox(message("settings.accept.pre.release.versions"))
          .bindSelected(
            { giteaSettings.acceptPreReleaseVersions },
            { giteaSettings.acceptPreReleaseVersions = it })
          .comment(message("settings.accept.pre.release.versions.comment"))
      }
      addWarningForMemoryOnlyPasswordSafeAndGet(
        scope,
        service<GiteaAccountManager>().canPersistCredentials,
        ::panel
      ).align(AlignX.LEFT)
    }
  }
}

@ApiStatus.Internal
@Service(Service.Level.APP)
@State(
  name = "GiteaSettings",
  storages = [Storage("gitea.xml")],
  category = SettingsCategory.TOOLS
)
@Suppress("UnstableApiUsage")
class GiteaSettings : CollectableSerializablePersistentStateComponent<GiteaSettings.State>(State()) {
  // Without a generated serializer the platform silently saves and loads nothing for this state.
  @Serializable
  data class State(
    val connectionTimeout: Int = 5_000,
    val cloneWithSsh: Boolean = false,
    val allUsersArePotentialReviewers : Boolean = false,
    /** Accept Gitea dev builds and release candidates, not only releases — see [GiteaServersManager.isSupported]. */
    val acceptPreReleaseVersions: Boolean = false,
    /** [GiteaAccount.id] -> whether the Request Review picker should offer every user on the
     * instance rather than just the repo's collaborators. Default (absent) is collaborators-only. */
  )

  var allUsersArePotentialReviewers : Boolean
    get() = state.allUsersArePotentialReviewers
    set(value) {
      updateState { it.copy(allUsersArePotentialReviewers = value) }
    }

  var acceptPreReleaseVersions: Boolean
    get() = state.acceptPreReleaseVersions
    set(value) {
      updateStateAndEmit { it.copy(acceptPreReleaseVersions = value) }
    }

  /** [acceptPreReleaseVersions] as a flow, so Gitea detection follows a change of it right away. */
  val acceptPreReleaseVersionsState: StateFlow<Boolean> = stateFlow.mapState { it.acceptPreReleaseVersions }

  var connectionTimeout: Int
    get() = state.connectionTimeout
    set(value) {
      updateState { it.copy(connectionTimeout = value) }
    }

  var cloneWithSsh: Boolean
    get() = state.cloneWithSsh
    set(value) {
      updateState { it.copy(cloneWithSsh = value) }
    }

  companion object {
    fun getInstance(): GiteaSettings =
      ApplicationManager.getApplication().service<GiteaSettings>()
  }
}