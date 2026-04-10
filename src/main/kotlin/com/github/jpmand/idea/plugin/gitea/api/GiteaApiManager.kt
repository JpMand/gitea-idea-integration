package com.github.jpmand.idea.plugin.gitea.api

abstract class GiteaApiManager {
  fun getClient(server: GiteaServerPath, token: String): GiteaApi =
    getClient(server) { token }

  fun getClient(server: GiteaServerPath, tokenSupplier: () -> String): GiteaApi =
    GiteaApiImpl(server, tokenSupplier)

  fun getUnauthenticatedClient(server: GiteaServerPath): GiteaApi =
    GiteaApiImpl(server)
}

class GiteaApiManagerImpl : GiteaApiManager()