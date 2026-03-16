package com.github.jpmand.idea.plugin.gitea.api.rest

import com.github.jpmand.idea.plugin.gitea.api.GiteaApi
import com.github.jpmand.idea.plugin.gitea.api.rest.models.GiteaMarkdownOptionDTO
import com.github.jpmand.idea.plugin.gitea.api.rest.models.GiteaServerVersionDTO
import com.intellij.collaboration.api.json.loadJsonValue
import com.intellij.collaboration.util.resolveRelative

@Suppress("UnstableApiUsage")
suspend fun GiteaApi.getServerVersion(): GiteaServerVersionDTO {
  val uri = server.restApiUri().resolveRelative("version")
  val request = request(uri).GET().build()
  return rest.loadJsonValue<GiteaServerVersionDTO>(request).body()
}

@Suppress("UnstableApiUsage")
suspend fun GiteaApi.renderMarkdownAsHtml(context: String, mode: String, text: String): String {
  val uri = server.restApiUri().resolveRelative("markdown")
  val body = GiteaMarkdownOptionDTO(context, mode, text)
  val request = request(uri).POST(rest.jsonBodyPublisher(uri, body)).build()
  return rest.loadJsonValue<String>(request).body()
}

@Suppress("UnstableApiUsage")
suspend fun GiteaApi.renderRawMarkdownAsHtml(text: String): String {
  val uri = server.restApiUri().resolveRelative("markdown").resolveRelative("raw")
  val request = request(uri).POST(rest.jsonBodyPublisher(uri, text)).build()
  return rest.loadJsonValue<String>(request).body()
}