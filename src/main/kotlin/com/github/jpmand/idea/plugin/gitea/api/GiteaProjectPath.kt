package com.github.jpmand.idea.plugin.gitea.api

import com.intellij.openapi.util.NlsSafe
import com.intellij.util.text.nullize

data class GiteaProjectPath(val owner: @NlsSafe String, val name: @NlsSafe String) {
  @NlsSafe
  override fun toString(): String = "$owner/$name"

  @NlsSafe
  fun fullPath(withOwner: Boolean = true): String = if (withOwner) "$owner/$name" else name

  companion object {
    fun create(server: GiteaServerPath, remoteUrl: String): GiteaProjectPath? {
      val serverPath = server.toURI().path
      val remotePath = "";

      if (!remotePath.startsWith(serverPath)) {
        return null
      }
      val repoPath = remotePath.removePrefix(serverPath).removePrefix("/")
      return extractProjectPath(repoPath)
    }

    private fun extractProjectPath(repoPath: String): GiteaProjectPath? {
      val lastSep = repoPath.lastIndexOf('/')
      if (lastSep < 0) return null
      val name = repoPath.substringAfterLast('/', "").nullize() ?: return null
      val owner = repoPath.substringBeforeLast('/', "").nullize() ?: return null
      return GiteaProjectPath(owner, name)
    }
  }
}
