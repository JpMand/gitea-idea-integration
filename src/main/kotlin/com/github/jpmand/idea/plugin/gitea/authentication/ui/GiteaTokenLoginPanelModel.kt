package com.github.jpmand.idea.plugin.gitea.authentication.ui

import com.github.jpmand.idea.plugin.gitea.api.GiteaApiManager
import com.github.jpmand.idea.plugin.gitea.api.GiteaServerPath
import com.github.jpmand.idea.plugin.gitea.api.rest.currentUser
import com.github.jpmand.idea.plugin.gitea.authentication.GiteLoginUtil
import com.intellij.collaboration.auth.ui.login.LoginException
import com.intellij.collaboration.auth.ui.login.LoginPanelModelBase
import com.intellij.collaboration.auth.ui.login.LoginTokenGenerator
import com.intellij.collaboration.util.URIUtil
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.components.service
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext

class GiteaTokenLoginPanelModel(
  var requiredUsername: String? = null,
  var uniqueAccountPredicate: (GiteaServerPath, String) -> Boolean
) : LoginPanelModelBase(), LoginTokenGenerator {

  private val _tryGitAuthorizationSignal: MutableSharedFlow<Unit> = MutableSharedFlow(replay = 1)
  val tryGitAuthorizationSignal: Flow<Unit> = _tryGitAuthorizationSignal.asSharedFlow()

  override suspend fun checkToken(): String {
    val server = createServerPath(serverUri)
    val api = service<GiteaApiManager>().getClient(server, token)
    val user = withContext(Dispatchers.IO) {
      api.currentUser().body()
    }
    val username = user.name
    val _requiredUsername = requiredUsername
    if (_requiredUsername != null && _requiredUsername != username) {
      throw LoginException.AccountUsernameMismatch(_requiredUsername, username)
    }
    if (!uniqueAccountPredicate(server, username)) {
      throw LoginException.AccountAlreadyExists(username)
    }

    return username;
  }

  override fun canGenerateToken(serverUri: String): Boolean {
    return URIUtil.isValidHttpUri(serverUri)
  }

  override fun generateToken(serverUri: String) {
    val newTokenUrl = GiteLoginUtil.buildNewTokenUrl(serverUri)?: return
    BrowserUtil.browse(newTokenUrl)
  }

  suspend fun tryGitAuthorization() {
    _tryGitAuthorizationSignal.emit(Unit)
  }

  fun getServerPath(): GiteaServerPath = createServerPath(serverUri)

  private fun createServerPath(serverUri: String): GiteaServerPath {
    val normalized = URIUtil.normalizeAndValidateHttpUri(serverUri)
    return GiteaServerPath.from(normalized)
  }
}