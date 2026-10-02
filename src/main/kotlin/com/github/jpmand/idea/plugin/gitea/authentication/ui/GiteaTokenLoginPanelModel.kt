package com.github.jpmand.idea.plugin.gitea.authentication.ui

import com.github.jpmand.idea.plugin.gitea.GiteaServersManager
import com.github.jpmand.idea.plugin.gitea.api.GiteaApiManager
import com.github.jpmand.idea.plugin.gitea.api.GiteaServerPath
import com.github.jpmand.idea.plugin.gitea.api.giteaApiCall
import com.github.jpmand.idea.plugin.gitea.api.rest.currentUser
import com.github.jpmand.idea.plugin.gitea.authentication.GiteLoginUtil
import com.intellij.collaboration.auth.ui.login.LoginException
import com.intellij.collaboration.auth.ui.login.LoginPanelModelBase
import com.intellij.collaboration.auth.ui.login.LoginTokenGenerator
import com.intellij.collaboration.util.URIUtil
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext

private val LOG = logger<GiteaTokenLoginPanelModel>()

@Suppress("UnstableApiUsage")
class GiteaTokenLoginPanelModel(
  var requiredUsername: String? = null,
  var uniqueAccountPredicate: (GiteaServerPath, String) -> Boolean
) : LoginPanelModelBase(), LoginTokenGenerator {

  private val _tryGitAuthorizationSignal: MutableSharedFlow<Unit> = MutableSharedFlow(replay = 1)
  val tryGitAuthorizationSignal: Flow<Unit> = _tryGitAuthorizationSignal.asSharedFlow()

  override suspend fun checkToken(): String {
    val server = createServerPath(serverUri)
    LOG.info("Checking a token against $server")
    try {
      return checkToken(server).also { LOG.info("Token for $server belongs to $it") }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      // The dialog shows the reason; a rejected token or an old server is a user error, not a bug.
      LOG.info("Token check against $server failed: ${e.javaClass.simpleName}: ${e.message}")
      throw e
    }
  }

  private suspend fun checkToken(server: GiteaServerPath): String {
    val api = service<GiteaApiManager>().getClient(server, token)
    val serversManager = service<GiteaServersManager>()
    // giteaApiCall: a rejected token reads "invalid or expired token", not a raw HTTP dump.
    val metadata = withContext(Dispatchers.IO) { giteaApiCall { serversManager.getMetadata(api) } }
    LOG.debug("$server runs Gitea ${metadata.version}")
    if (metadata.version < serversManager.earliestSupportedVersion) {
      throw LoginException.UnsupportedServerVersion(serversManager.earliestSupportedVersion.toString())
    }
    val user = withContext(Dispatchers.IO) {
      giteaApiCall { api.currentUser() }
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