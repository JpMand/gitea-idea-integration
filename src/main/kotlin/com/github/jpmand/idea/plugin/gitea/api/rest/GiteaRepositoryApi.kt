package com.github.jpmand.idea.plugin.gitea.api.rest

import com.github.jpmand.idea.plugin.gitea.api.GiteaApi
import com.github.jpmand.idea.plugin.gitea.api.GiteaUriUtil
import com.github.jpmand.idea.plugin.gitea.api.rest.dto.Repository
import com.github.jpmand.idea.plugin.gitea.api.rest.dto.SearchResults
import com.intellij.collaboration.api.json.loadJsonValue
import com.intellij.collaboration.util.resolveRelative

@Suppress("UnstableApiUsage")
suspend fun GiteaApi.getRepository(owner: String, name: String): Repository? {
  val uri = server.restApiUri().resolveRelative("repos/$owner/$name")
  val request = request(uri).GET().build()
  return rest.loadJsonValue<Repository>(request).body()
}

@Suppress("UnstableApiUsage")
suspend fun GiteaApi.repoSearch(
  keyword: String,
  topic: Boolean?,
  includeDesc: Boolean?,
  uid: Int?,
  priorityOwnerId: Int?,
  teamId: Int?,
  starredBy: Int?,
  private: Boolean?,
  isPrivate: Boolean?,
  template: Boolean?,
  archived: Boolean?,
  mode: String?,
  exclusive: Boolean?,
  sort: String?,
  page: Int?,
  limit: Int?
): SearchResults? {
  val baseUri = server.restApiUri().resolveRelative("repos/search")
  val uri = GiteaUriUtil.QueryBuilder()
    .addParam("q", keyword)
    .addParam("topic", topic)
    .addParam("include_desc", includeDesc)
    .addParam("uid", uid)
    .addParam("priority_owner_id", priorityOwnerId)
    .addParam("team_id", teamId)
    .addParam("starred_by", starredBy)
    .addParam("private", private)
    .addParam("is_private", isPrivate)
    .addParam("template", template)
    .addParam("archived", archived)
    .addParam("mode", mode)
    .addParam("exclusive", exclusive)
    .addParam("sort", sort)
    .addParam("page", page)
    .addParam("limit", limit)
    .build(baseUri)
  val request = request(uri).GET().build()
  return rest.loadJsonValue<SearchResults>(request).body()
}
