package com.github.jpmand.idea.plugin.gitea.api.rest

import com.github.jpmand.idea.plugin.gitea.api.GiteaApi
import com.github.jpmand.idea.plugin.gitea.api.models.GiteaUser
import com.intellij.collaboration.api.json.loadJsonValue
import com.intellij.collaboration.util.resolveRelative
import java.awt.Image
import java.net.URI
import java.net.http.HttpResponse

suspend fun GiteaApi.currentUser(): HttpResponse<out GiteaUser> {
  val uri = server.restApiUri().resolveRelative("user")
  val request = request(uri).GET().build()
  return rest.loadJsonValue(request)
}

suspend fun GiteaApi.loadImage(uri : String) : Image {
  val request = request(uri).GET().build()
  return loadImage(request).body()
}


suspend fun GiteaApi.loadImage(uri : URI) : Image {
  val request = request(uri).GET().build()
  return loadImage(request).body()
}