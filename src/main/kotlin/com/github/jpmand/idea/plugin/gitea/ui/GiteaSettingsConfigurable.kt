package com.github.jpmand.idea.plugin.gitea.ui

import com.github.jpmand.idea.plugin.gitea.api.GiteaApiManager
import com.github.jpmand.idea.plugin.gitea.authentication.account.GiteaAccountManager
import com.github.jpmand.idea.plugin.gitea.authentication.account.GiteaProjectDefaultAccountHolder
import com.github.jpmand.idea.plugin.gitea.authentication.ui.GiteaAccountsDetailsProvider
import com.github.jpmand.idea.plugin.gitea.authentication.ui.GiteaAccountsListModel
import com.github.jpmand.idea.plugin.gitea.authentication.ui.GiteaAccountsPanelActionsController
import com.github.jpmand.idea.plugin.gitea.util.GiteaPluginProjectScopeProvider
import com.github.jpmand.idea.plugin.gitea.util.GiteaSettings
import com.github.jpmand.idea.plugin.gitea.util.GiteaUtil.SERVICE_DISPLAY_NAME
import com.intellij.collaboration.auth.ui.AccountsPanelFactory
import com.intellij.collaboration.auth.ui.AccountsPanelFactory.Companion.addWarningForMemoryOnlyPasswordSafeAndGet
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.asContextElement
import com.intellij.openapi.components.service
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.panel
import kotlinx.coroutines.Dispatchers

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
        checkBox("settings.automatically.mark.as.viewed")
          .bindSelected(
            { giteaSettings.isAutomaticallyMarkAsViewed },
            { giteaSettings.isAutomaticallyMarkAsViewed = it })
      }

      addWarningForMemoryOnlyPasswordSafeAndGet(
        scope,
        service<GiteaAccountManager>().canPersistCredentials,
        ::panel
      ).align(AlignX.RIGHT)
    }
  }
}