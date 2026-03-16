package com.github.jpmand.idea.plugin.gitea.util

import com.github.jpmand.idea.plugin.gitea.api.GiteaRepositoryCoordinates
import com.github.jpmand.idea.plugin.gitea.api.GiteaServerPath
import git4idea.remote.GitRemoteUrlCoordinates
import git4idea.remote.hosting.HostedGitRepositoryMapping
import git4idea.repo.GitRemote
import git4idea.repo.GitRepository
import git4idea.ui.branch.GitRepositoryMappingData

@Suppress("UnstableApiUsage")
data class GiteaGitRepositoryMapping(
  override val repository: GiteaRepositoryCoordinates,
  override val remote: GitRemoteUrlCoordinates
) : GitRepositoryMappingData, HostedGitRepositoryMapping {
  override val gitRemote: GitRemote
    get() = remote.remote
  override val gitRepository: GitRepository
    get() = remote.repository
  override val repositoryPath: String
    get() = repository.repositoryPath.repository

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is GiteaGitRepositoryMapping) return false

    if (repository != other.repository) return false

    return true
  }

  override fun hashCode(): Int {
    return repository.hashCode()
  }

  override fun toString(): String {
    return "(repository=$repository, remote=$repository)"
  }

  companion object {
    fun create(server: GiteaServerPath, remote: GitRemoteUrlCoordinates): GiteaGitRepositoryMapping? {
      val repository = GiteaRepositoryCoordinates.create(server, remote) ?: return null
      return GiteaGitRepositoryMapping(repository, remote)
    }
  }
}