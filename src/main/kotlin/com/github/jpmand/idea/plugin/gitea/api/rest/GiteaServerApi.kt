package com.github.jpmand.idea.plugin.gitea.api.rest

import com.github.jpmand.idea.plugin.gitea.api.GiteaApi
import com.github.jpmand.idea.plugin.gitea.api.loadJsonValue
import com.github.jpmand.idea.plugin.gitea.api.rest.dto.ServerVersion
import com.intellij.collaboration.util.resolveRelative

@Suppress("UnstableApiUsage")
suspend fun GiteaApi.getServerVersion(): ServerVersion {
  val uri = server.restApiUri().resolveRelative("version")
  val request = request(uri).GET().build()
  return rest.loadJsonValue<ServerVersion>(request).body()
}
