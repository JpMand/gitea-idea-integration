package com.github.jpmand.idea.plugin.gitea.api

import com.github.jpmand.idea.plugin.gitea.GiteaServersManager
import com.intellij.openapi.components.service
import kotlin.getValue

abstract class GiteaApiManager {
  protected abstract val serversManager : GiteaServersManager
  fun getClient(server: GiteaServerPath, token: String): GiteaApi =
    getClient(server) { token }

  private fun getClient(server: GiteaServerPath, tokenSupplier: () -> String): GiteaApi =
    GiteaApiImpl(server, tokenSupplier)

  fun getUnauthenticatedClient(server: GiteaServerPath): GiteaApi =
    GiteaApiImpl(server)
}

class GiteaApiManagerImpl : GiteaApiManager() {
  override val serversManager: GiteaServersManager by lazy { service<GiteaServersManager>() }
}