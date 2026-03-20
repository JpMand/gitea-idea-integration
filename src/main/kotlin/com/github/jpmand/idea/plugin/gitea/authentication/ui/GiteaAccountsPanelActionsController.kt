package com.github.jpmand.idea.plugin.gitea.authentication.ui

import com.github.jpmand.idea.plugin.gitea.api.GiteaServerPath
import com.github.jpmand.idea.plugin.gitea.authentication.GiteLoginUtil
import com.github.jpmand.idea.plugin.gitea.authentication.GiteLoginUtil.LoginResult
import com.github.jpmand.idea.plugin.gitea.authentication.account.GiteaAccount
import com.github.jpmand.idea.plugin.gitea.util.GiteaBundle.message
import com.intellij.collaboration.auth.ui.AccountsPanelActionsController
import com.intellij.openapi.project.Project
import com.intellij.ui.awt.RelativePoint
import com.intellij.util.asSafely
import javax.swing.JComponent

/**
 * Actions controller for accounts panel in `Version Control -> Gitea` settings. Handles add and edit of accounts.
 */

@Suppress("UnstableApiUsage")
class GiteaAccountsPanelActionsController(
  private val project: Project,
  private val model: GiteaAccountsListModel
) : AccountsPanelActionsController<GiteaAccount> {
  override val isAddActionWithPopup: Boolean = false

  override fun addAccount(parentComponent: JComponent, point: RelativePoint?) {
    val loginResult = GiteLoginUtil.logInViaToken(
      project,
      parentComponent,
      loginSource = message( "gitea.login.source.settings"),
      uniqueAccountPredicate = ::isAccountUnique
    )
      .asSafely<LoginResult.Success>() ?: return
    model.add(loginResult.account, loginResult.token)
  }

  override fun editAccount(parentComponent: JComponent, account: GiteaAccount) {
    val loginResult = GiteLoginUtil.updateToken(
      project,
      parentComponent,
      account,
      loginSource = message( "gitea.login.source.settings"),
      uniqueAccountPredicate = ::isAccountUnique
    )
      .asSafely<LoginResult.Success>() ?: return
    model.update(account, loginResult.token)
  }

  private fun isAccountUnique(server: GiteaServerPath, username: String): Boolean =
    GiteLoginUtil.isAccountUnique(model.accounts, server, username)
}