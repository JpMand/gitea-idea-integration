package com.github.jpmand.idea.plugin.gitea.api.rest

import com.github.jpmand.idea.plugin.gitea.api.GiteaApi
import com.github.jpmand.idea.plugin.gitea.api.rest.dto.MarkdownOption
import com.github.jpmand.idea.plugin.gitea.api.rest.dto.ServerVersion
import com.intellij.collaboration.api.httpclient.HttpClientUtil
import com.intellij.collaboration.api.json.loadJsonValue
import com.intellij.collaboration.util.resolveRelative

@Suppress("UnstableApiUsage")
suspend fun GiteaApi.getServerVersion(): ServerVersion {
  val uri = server.restApiUri().resolveRelative("version")
  val request = request(uri).GET().build()
  return rest.loadJsonValue<ServerVersion>(request).body()
}

suspend fun GiteaApi.Rest.checkIsGiteaServer() : Boolean =
  renderRawMarkdownAsHtml("*test*").isNotBlank()

@Suppress("UnstableApiUsage")
suspend fun GiteaApi.renderMarkdownAsHtml(context: String, mode: String, text: String): String {
  val uri = server.restApiUri().resolveRelative("markdown")
  val body = MarkdownOption(context, mode, text)
  val request = request(uri).POST(rest.jsonBodyPublisher(uri, body))
    .setHeader(HttpClientUtil.CONTENT_TYPE_HEADER, HttpClientUtil.CONTENT_TYPE_JSON)
    .build()
  return rest.loadJsonValue<String>(request).body()
}

@Suppress("UnstableApiUsage")
suspend fun GiteaApi.renderRawMarkdownAsHtml(text: String): String {
  val uri = server.restApiUri().resolveRelative("markdown").resolveRelative("raw")
  val request = request(uri).POST(rest.jsonBodyPublisher(uri, text)).build()
  return rest.loadJsonValue<String>(request).body()
}