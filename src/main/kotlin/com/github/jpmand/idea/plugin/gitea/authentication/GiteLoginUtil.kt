package com.github.jpmand.idea.plugin.gitea.authentication

import com.github.jpmand.idea.plugin.gitea.api.GiteaServerPath
import com.github.jpmand.idea.plugin.gitea.authentication.account.GiteaAccount
import com.github.jpmand.idea.plugin.gitea.authentication.account.GiteaProjectDefaultAccountHolder
import com.github.jpmand.idea.plugin.gitea.authentication.ui.GiteaChooseAccountDialog
import com.github.jpmand.idea.plugin.gitea.authentication.ui.GiteaTokenLoginPanelModel
import com.github.jpmand.idea.plugin.gitea.util.GiteaBundle
import com.github.jpmand.idea.plugin.gitea.util.GiteaPluginProjectScopeProvider
import com.intellij.collaboration.auth.ui.login.LoginModel
import com.intellij.collaboration.auth.ui.login.TokenLoginDialog
import com.intellij.collaboration.auth.ui.login.TokenLoginInputPanelFactory
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.util.NlsContexts
import com.intellij.util.Urls.parseEncoded
import com.intellij.util.asSafely
import com.intellij.util.concurrency.annotations.RequiresEdt
import org.jetbrains.annotations.Nls
import java.awt.Component
import javax.swing.JComponent

object GiteLoginUtil {

  internal fun buildNewTokenUrl(serverUri: String): String? {
    //Pre-filled grained token generation is not supported. So only redirect to token page
    return parseEncoded("${serverUri}/settings/applications")?.toExternalForm()
  }

  @RequiresEdt
  fun logInViaToken(
    project: Project,
    parentComponent: JComponent?,
    loginSource: String? = null,
    uniqueAccountPredicate: (GiteaServerPath, String) -> Boolean
  ): LoginResult =
    logInViaToken(project, parentComponent, GiteaServerPath.DEFAULT_SERVER, null, loginSource, uniqueAccountPredicate)

  @RequiresEdt
  fun updateToken(
    project: Project,
    parentComponent: JComponent?,
    account: GiteaAccount,
    loginSource: String? = null,
    uniqueAccountPredicate: (GiteaServerPath, String) -> Boolean
  ): LoginResult = updateToken(project, parentComponent, account, null, loginSource, uniqueAccountPredicate)

  @RequiresEdt
  internal fun logInViaToken(
    project: Project,
    parentComponent: JComponent?,
    serverPath: GiteaServerPath = GiteaServerPath.DEFAULT_SERVER,
    requiredUsername: String? = null,
    loginSource: String?,
    uniqueAccountPredicate: (GiteaServerPath, String) -> Boolean
  ): LoginResult {
    val model = GiteaTokenLoginPanelModel(requiredUsername, uniqueAccountPredicate).apply {
      serverUri = serverPath.toURI().toString()
    }
    val dialogTitle = GiteaBundle.message("account.add.dialog.title")
    val exitCode = showLoginDialog(project, parentComponent, model, dialogTitle, false)
    return when (exitCode) {
      DialogWrapper.OK_EXIT_CODE -> {
        val loginResult =
          model.loginState.value.asSafely<LoginModel.LoginState.Connected>() ?: return LoginResult.Failure
        LoginResult.Success(
          GiteaAccount(name = loginResult.username, server = model.getServerPath()),
          model.token
        )
      }

      DialogWrapper.NEXT_USER_EXIT_CODE -> LoginResult.OtherMethod
      else -> LoginResult.Failure
    }
  }

  @RequiresEdt
  internal fun updateToken(
    project: Project,
    parentComponent: JComponent?,
    account: GiteaAccount,
    requiredUsername: String? = null,
    loginSource: String? = null,
    uniqueAccountPredicate: (GiteaServerPath, String) -> Boolean
  ): LoginResult {
    val predicateWithoutCurrent: (GiteaServerPath, String) -> Boolean = { serverPath, username ->
      if (serverPath == account.server && username == account.name) true
      else uniqueAccountPredicate(serverPath, username)
    }

    val model = GiteaTokenLoginPanelModel(requiredUsername, predicateWithoutCurrent).apply {
      serverUri = account.server.toURI().toString()
    }
    val title = GiteaBundle.message("account.update.dialog.title")
    val exitState = showLoginDialog(project, parentComponent, model, title, true)
    val loginState = model.loginState.value
    if (exitState == DialogWrapper.OK_EXIT_CODE && loginState is LoginModel.LoginState.Connected) {
      return LoginResult.Success(
        GiteaAccount(id = account.id, name = loginState.username, server = model.getServerPath()),
        model.token
      )
    }

    return LoginResult.Failure
  }

  @RequiresEdt
  private fun showLoginDialog(
    project: Project,
    parentComponent: JComponent?,
    model: GiteaTokenLoginPanelModel,
    title: @NlsContexts.DialogTitle String,
    serverFieldDisabled: Boolean
  ): Int {
    val scopeProvider = project.service<GiteaPluginProjectScopeProvider>()
    val dialog = scopeProvider.constructDialog("Gitea Token login dialog") {
      TokenLoginDialog(project, this, parentComponent, model, title, model.tryGitAuthorizationSignal) {
        val cs = this
        TokenLoginInputPanelFactory(model).createIn(
          cs,
          serverFieldDisabled,
          tokenNote = GiteaBundle.message("clone.dialog.insufficient.scopes"),
          errorPresenter = GiteaLoginErrorStatusPresenter(cs, model)
        )
      }
    }
    dialog.showAndGet()

    return dialog.exitCode
  }

  @RequiresEdt
  internal fun chooseAccount(
    project: Project,
    parentComponent: Component?,
    description: @Nls String?,
    accounts: Collection<GiteaAccount>
  ): GiteaAccount? {
    val dialog =
      GiteaChooseAccountDialog(project, parentComponent, accounts, description, showHosts = false, allowDefault = true)
    return if (dialog.showAndGet()) {
      val account = dialog.myAccount
      if (dialog.mySetDefault) {
        project.service<GiteaProjectDefaultAccountHolder>().account = account
      }
      account
    } else null
  }

  fun isAccountUnique(accounts: Collection<GiteaAccount>, server: GiteaServerPath, username: String): Boolean =
    accounts.none { it.server.toURI() == server.toURI() && it.name == username }

  sealed interface LoginResult {
    data class Success(val account: GiteaAccount, val token: String) : LoginResult
    data object Failure : LoginResult
    data object OtherMethod : LoginResult
  }
}
