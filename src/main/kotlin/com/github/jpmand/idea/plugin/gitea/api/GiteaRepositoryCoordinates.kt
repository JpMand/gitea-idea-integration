package com.github.jpmand.idea.plugin.gitea.api

import com.intellij.openapi.util.NlsSafe
import git4idea.remote.GitRemoteUrlCoordinates
import git4idea.remote.hosting.HostedRepositoryCoordinates
import java.net.URI

@Suppress("UnstableApiUsage")
class GiteaRepositoryCoordinates(
  override val serverPath: GiteaServerPath,
  val repositoryPath: GiteaRepositoryPath
) : HostedRepositoryCoordinates {

  override fun getWebURI(): URI = serverPath.toURI().resolve(repositoryPath.fullPath())

  @NlsSafe
  override fun toString(): String {
    return "$serverPath/$repositoryPath"
  }

  companion object {
    fun create(serverPath: GiteaServerPath, remote: GitRemoteUrlCoordinates): GiteaRepositoryCoordinates? {
      val repositoryPath = GiteaRepositoryPath.create(serverPath, remote.url) ?: return null
      return GiteaRepositoryCoordinates(serverPath, repositoryPath)
    }
  }
}