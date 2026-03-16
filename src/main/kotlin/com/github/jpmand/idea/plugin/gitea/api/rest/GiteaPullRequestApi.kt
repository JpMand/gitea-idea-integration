package com.github.jpmand.idea.plugin.gitea.api.rest

import com.github.jpmand.idea.plugin.gitea.api.GiteaApi
import com.github.jpmand.idea.plugin.gitea.api.GiteaUriUtil
import com.github.jpmand.idea.plugin.gitea.api.rest.models.pr.GiteaPullRequestDTO
import com.intellij.collaboration.api.json.loadJsonValue
import com.intellij.collaboration.util.resolveRelative

suspend fun GiteaApi.repoListPullRequests(
  owner: String,
  repo: String,
  baseBranch: String?,
  state: String?,
  sort: String?,
  milestone: Int?,
  labels: List<String>?,
  poster: String?,
  page: Int?,
  limit: Int?,
) : List<GiteaPullRequestDTO>
{
  var uri = GiteaUriUtil.QueryBuilder()
    .addParam("state", state)
    .addParam("sort", sort)
    .addParam("milestone", milestone)
    .addParam("labels", labels?.joinToString(","))
    .addParam("poster", poster)
    .addParam("page", page)
    .addParam("limit", limit)
    .build(server.restApiUri().resolveRelative("repos/$owner/$repo/pulls"))
  val request = request(uri).GET().build()
  return rest.loadJsonValue<List<GiteaPullRequestDTO>>(request).body()
}

suspend fun GiteaApi.repoListPinnedPullRequests(owner: String, repo: String): List<GiteaPullRequestDTO> {
  var uri = server.restApiUri().resolveRelative("repos/$owner/$repo/pulls/pinned")
  val request = request(uri).GET().build()
  return rest.loadJsonValue<List<GiteaPullRequestDTO>>(request).body()
}