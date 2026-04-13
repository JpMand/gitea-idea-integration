package com.github.jpmand.idea.plugin.gitea.authentication.ui

import com.github.jpmand.idea.plugin.gitea.util.GiteaBundle
import com.github.jpmand.idea.plugin.gitea.api.GiteaApi
import com.github.jpmand.idea.plugin.gitea.api.models.GiteaUser
import com.github.jpmand.idea.plugin.gitea.api.rest.currentUser
import com.github.jpmand.idea.plugin.gitea.api.rest.loadImage
import com.github.jpmand.idea.plugin.gitea.authentication.account.GiteaAccount
import com.github.jpmand.idea.plugin.gitea.authentication.account.GiteaAccountManager
import com.intellij.collaboration.auth.ui.LazyLoadingAccountsDetailsProvider
import com.intellij.collaboration.auth.ui.cancelOnRemoval
import com.intellij.collaboration.util.ResultUtil.runCatchingUser
import com.intellij.util.io.URLUtil
import icons.CollaborationToolsIcons
import kotlinx.coroutines.CoroutineScope
import java.awt.Image
import kotlin.coroutines.cancellation.CancellationException

class GiteaAccountsDetailsProvider(
  scope: CoroutineScope,
  private val apiSupplier: suspend (GiteaAccount) -> GiteaApi?
) : LazyLoadingAccountsDetailsProvider<GiteaAccount, GiteaUser>(scope, CollaborationToolsIcons.Review.DefaultAvatar) {

  constructor(
    scope: CoroutineScope,
    accountsModel: GiteaAccountsListModel,
    apiSupplier: suspend (GiteaAccount) -> GiteaApi?
  )
          : this(scope, apiSupplier) {
    cancelOnRemoval(accountsModel.accountsListModel)
  }

  constructor(
    scope: CoroutineScope,
    accountManager: GiteaAccountManager,
    apiSupplier: suspend (GiteaAccount) -> GiteaApi?
  )
          : this(scope, apiSupplier) {
    cancelOnRemoval(scope, accountManager)
  }

  override suspend fun loadDetails(account: GiteaAccount): Result<GiteaUser> {
    try {
      val api = apiSupplier(account) ?: return Result.Error(GiteaBundle.message("account.token.missing"), true)
      val details = runCatchingUser {
        api.currentUser()
      }.getOrElse {
        return Result.Error(it.localizedMessage, false)
      }
      return Result.Success(details)
    } catch (ce: CancellationException) {
      throw ce
    } catch (e: Exception) {
      return Result.Error(e.message, false)
    }
  }

  override suspend fun loadAvatar(
    account: GiteaAccount,
    url: String
  ): Image? {
    val api = apiSupplier(account) ?: return null
    val imageUrl = if (url.startsWith(URLUtil.HTTP_PROTOCOL)) url else account.server.toString() + url

    return api.loadImage( imageUrl)
  }
}