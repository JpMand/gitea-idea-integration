package com.github.jpmand.idea.plugin.gitea.authentication

import com.github.jpmand.idea.plugin.gitea.authentication.account.GiteaAccount
import com.intellij.openapi.project.Project
import com.intellij.util.concurrency.annotations.RequiresEdt
import javax.swing.JComponent

class GiteLoginUtil {

  @RequiresEdt
  fun logInViaToken(
    project: Project, parentComponent : JComponent?,
    uniqueAccountPredicate: (GiteaAccount, String) -> Boolean
  ): LoginResult = TODO()


  sealed interface LoginResult {
    data class Success(val account: GiteaAccount, val token : String): LoginResult
    data object Failure : LoginResult
    data object OtherMethod : LoginResult
  }
}