package com.github.jpmand.idea.plugin.gitea.authentication.extensions

import com.github.jpmand.idea.plugin.gitea.authentication.account.GiteaAccount
import com.github.jpmand.idea.plugin.gitea.authentication.ui.GiteaChooseAccountDialog
import com.intellij.openapi.project.Project
import com.intellij.util.AuthData
import com.intellij.util.concurrency.annotations.RequiresEdt
import git4idea.DialogManager
import git4idea.remote.InteractiveGitHttpAuthDataProvider
import java.awt.Component

class GiteaSelectAccountGitHttpAuthDataProvider(private val project : Project, val accounts: Map<GiteaAccount, String?>)
  : InteractiveGitHttpAuthDataProvider {

  @RequiresEdt
  override fun getAuthData(parentComponent: Component?): AuthData? {
    val (account, setDefault) = chooseAccount(parentComponent) ?: return null
    val token = accounts[account] ?: TODO("Create GiteaAccountsUtil.requestNewToken") ?: return null
    if (setDefault) {
      TODO("Create GiteaAccountsUtil.SetDefaultAccount")
    }
    return TODO("create GiteaAuthData")
  }

    private fun chooseAccount(parentComponent: Component?): Pair<GiteaAccount, Boolean>? {
       val dialog = GiteaChooseAccountDialog(
         project,
         parentComponent,
         accounts.keys,
         null,
         false,
         true,
         "account.choose.title",
         "login.dialog.button.login"
       )
      DialogManager.show(dialog)
      return if (dialog.isOK) dialog.myAccount to dialog.mySetDefault else null
    }
}