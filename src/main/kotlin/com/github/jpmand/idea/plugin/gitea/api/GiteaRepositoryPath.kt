package com.github.jpmand.idea.plugin.gitea.api

import com.intellij.openapi.util.NlsSafe
import com.intellij.util.text.nullize
import git4idea.remote.hosting.GitHostingUrlUtil

data class GiteaRepositoryPath(val owner: @NlsSafe String, val repository: @NlsSafe String) {
  @NlsSafe
  override fun toString(): String = "$owner/$repository"

  @NlsSafe
  fun fullPath(withOwner: Boolean = true): String = if (withOwner) "$owner/$repository" else repository

  companion object {
    fun create(server: GiteaServerPath, remoteUrl: String): GiteaRepositoryPath? {
      val serverPath = server.toURI().path
      val remotePath = GitHostingUrlUtil.getUriFromRemoteUrl(remoteUrl)?.path?: return null

      if (!remotePath.startsWith(serverPath)) {
        return null
      }
      val repoPath = remotePath.removePrefix(serverPath).removePrefix("/")
      return extractProjectPath(repoPath)
    }

    private fun extractProjectPath(repoPath: String): GiteaRepositoryPath? {
      val lastSep = repoPath.lastIndexOf('/')
      if (lastSep < 0) return null
      val repository = repoPath.substringAfterLast('/', "").nullize() ?: return null
      val owner = repoPath.substringBeforeLast('/', "").nullize() ?: return null
      return GiteaRepositoryPath(owner, repository)
    }
  }
}
