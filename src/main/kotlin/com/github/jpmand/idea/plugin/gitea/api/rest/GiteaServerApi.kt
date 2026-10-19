package com.github.jpmand.idea.plugin.gitea.api.rest

import com.github.jpmand.idea.plugin.gitea.api.GiteaApi
import com.github.jpmand.idea.plugin.gitea.api.rest.dto.MarkdownOption
import com.github.jpmand.idea.plugin.gitea.api.rest.dto.ServerVersion
import com.intellij.collaboration.api.json.loadJsonValue
import com.intellij.collaboration.util.resolveRelative
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Suppress("UnstableApiUsage")
suspend fun GiteaApi.getServerVersion(): ServerVersion {
  val uri = server.restApiUri().resolveRelative("version")
  val request = request(uri).GET().build()
  return withContext(Dispatchers.IO){
    with(rest){
      request.loadJsonValue<ServerVersion>()
    }
  }.body()
}

@Suppress("UnstableApiUsage")
suspend fun GiteaApi.renderMarkdownAsHtml(context: String, mode: String, text: String): String {
  val uri = server.restApiUri().resolveRelative("markdown")
  val body = MarkdownOption(context, mode, text)
  val request = rest.postJson(uri, body).build()
  return withContext(Dispatchers.IO){
    with(rest){
      request.loadJsonValue<String>()
    }
  }.body()
}

@Suppress("UnstableApiUsage")
suspend fun GiteaApi.renderRawMarkdownAsHtml(text: String): String {
  val uri = server.restApiUri().resolveRelative("markdown").resolveRelative("raw")
  val request = rest.postJson(uri, text).build()
  return withContext(Dispatchers.IO){
    with(rest){
      request.loadJsonValue<String>()
    }
  }.body()
}