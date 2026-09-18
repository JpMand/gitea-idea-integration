package com.github.jpmand.idea.plugin.gitea.api.rest

import com.github.jpmand.idea.plugin.gitea.api.GiteaApi
import com.github.jpmand.idea.plugin.gitea.api.GiteaUriUtil
import com.github.jpmand.idea.plugin.gitea.api.models.GiteaUser
import com.github.jpmand.idea.plugin.gitea.api.rest.dto.Repository
import com.github.jpmand.idea.plugin.gitea.api.rest.dto.User
import com.intellij.collaboration.api.json.loadJsonList
import com.intellij.collaboration.api.json.loadJsonValue
import com.intellij.collaboration.util.resolveRelative
import java.awt.Image

@Suppress("UnstableApiUsage")
suspend fun GiteaApi.currentUser(): GiteaUser {
  val uri = server.restApiUri().resolveRelative("user")
  val request = request(uri).GET().build()
  return GiteaUser.fromDto(rest.loadJsonValue<User>(request).body())
}

@Suppress("UnstableApiUsage")
suspend fun GiteaApi.userCurrentListRepos(page: Int, limit: Int): Collection<Repository> {
  val uri = GiteaUriUtil.QueryBuilder()
    .addParam("page", page)
    .addParam("limit", limit)
    .build(server.restApiUri().resolveRelative("user/repos"))
  val request = request(uri).GET().build()
  return rest.loadJsonList<Repository>(request).body()
}

@Suppress("UnstableApiUsage")
suspend fun GiteaApi.loadImage(uri: String): Image {
  val request = request(uri).GET().build()
  return loadImage(request).body()
}
