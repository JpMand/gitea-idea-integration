package com.github.jpmand.idea.plugin.gitea.ui

import com.github.jpmand.idea.plugin.gitea.util.GiteaBundle.message
import com.github.jpmand.idea.plugin.gitea.api.GiteaApiManager
import com.github.jpmand.idea.plugin.gitea.authentication.account.GiteaAccountManager
import com.github.jpmand.idea.plugin.gitea.authentication.account.GiteaProjectDefaultAccountHolder
import com.github.jpmand.idea.plugin.gitea.authentication.ui.GiteaAccountsDetailsProvider
import com.github.jpmand.idea.plugin.gitea.authentication.ui.GiteaAccountsListModel
import com.github.jpmand.idea.plugin.gitea.authentication.ui.GiteaAccountsPanelActionsController
import com.github.jpmand.idea.plugin.gitea.util.GiteaPluginProjectScopeProvider
import com.github.jpmand.idea.plugin.gitea.util.GiteaUtil.SERVICE_DISPLAY_NAME
import com.intellij.collaboration.auth.ui.AccountsPanelFactory
import com.intellij.collaboration.auth.ui.AccountsPanelFactory.Companion.addWarningForMemoryOnlyPasswordSafeAndGet
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.asContextElement
import com.intellij.openapi.components.SerializablePersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.SettingsCategory
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.RightGap
import com.intellij.ui.dsl.builder.bindIntText
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.dsl.builder.panel
import kotlinx.coroutines.Dispatchers
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

    val scope = scopeProvider.createDisposedScope(
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

      row {
        checkBox(message("settings.automatically.mark.as.viewed"))
          .bindSelected(
            { giteaSettings.isAutomaticallyMarkAsViewed },
            { giteaSettings.isAutomaticallyMarkAsViewed = it })
      }

      row {
        message("settings.connection.timeout")
        intTextField(range = 0..60)
          .columns(2)
          .bindIntText({ giteaSettings.connectionTimeout / 1000 }, { giteaSettings.connectionTimeout = it * 1000 })
          .gap(RightGap.SMALL)
        @Suppress("DialogTitleCapitalization")
        label(message("settings.connection.timeout.seconds"))
          .gap(RightGap.COLUMNS)
      }


      addWarningForMemoryOnlyPasswordSafeAndGet(
        scope,
        service<GiteaAccountManager>().canPersistCredentials,
        ::panel
      ).align(AlignX.RIGHT)
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
class GiteaSettings : SerializablePersistentStateComponent<GiteaSettings.State>(State()) {
  @Serializable
  data class State(
    val automaticallyMarkAsViewed: Boolean = false,
    val connectionTimeout: Int = 5_000
  )

  var isAutomaticallyMarkAsViewed: Boolean
    get() = state.automaticallyMarkAsViewed
    set(value) {
      updateState { it.copy(automaticallyMarkAsViewed = value) }
    }

  var connectionTimeout: Int
    get() = state.connectionTimeout
    set(value) {
      updateState { it.copy(connectionTimeout = value) }
    }

  companion object {
    fun getInstance(): GiteaSettings =
      ApplicationManager.getApplication().service<GiteaSettings>()
  }
}