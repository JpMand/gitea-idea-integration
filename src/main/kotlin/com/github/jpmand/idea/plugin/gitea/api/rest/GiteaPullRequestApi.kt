package com.github.jpmand.idea.plugin.gitea.api.rest

import com.github.jpmand.idea.plugin.gitea.api.GiteaApi
import com.github.jpmand.idea.plugin.gitea.api.GiteaUriUtil
import com.github.jpmand.idea.plugin.gitea.api.rest.models.pr.GiteaPullRequestDTO
import com.github.jpmand.idea.plugin.gitea.api.rest.models.pr.GiteaPullRequestSortEnum
import com.github.jpmand.idea.plugin.gitea.api.rest.models.GiteaStateEnum
import com.intellij.collaboration.api.json.loadJsonValue
import com.intellij.collaboration.util.resolveRelative

/***
 * List a repo's pull requests
 *
 * @param owner Owner of the repo
 * @param repo Name of the repo
 * @param baseBranch Filter by target base branch of the pull request
 * @param state State of pull request
 * @param sort Type of sort
 * @param milestone ID of the milestone
 * @param labels Label IDs
 * @param poster Filter by pull request author
 * @param page Page number of results to return (1-based)
 * @param limit Page size of results
 */
@Suppress("UnstableApiUsage")
suspend fun GiteaApi.repoListPullRequests(
  owner: String,
  repo: String,
  baseBranch: String?,
  state: GiteaStateEnum?,
  sort: GiteaPullRequestSortEnum?,
  milestone: Int?,
  labels: List<String>?,
  poster: String?,
  page: Int?,
  limit: Int?,
): List<GiteaPullRequestDTO> {
  val uri = GiteaUriUtil.QueryBuilder()
    .addParam("base_branch", baseBranch)
    .addParam("state", state?.value)
    .addParam("sort", sort?.value)
    .addParam("milestone", milestone)
    .addParam("labels", labels?.joinToString(","))
    .addParam("poster", poster)
    .addParam("page", page)
    .addParam("limit", limit)
    .build(server.restApiUri().resolveRelative("repos/$owner/$repo/pulls"))
  val request = request(uri).GET().build()
  return rest.loadJsonValue<List<GiteaPullRequestDTO>>(request).body()
}

@Suppress("UnstableApiUsage")
suspend fun GiteaApi.repoListPinnedPullRequests(owner: String, repo: String): List<GiteaPullRequestDTO> {
  val uri = server.restApiUri().resolveRelative("repos/$owner/$repo/pulls/pinned")
  val request = request(uri).GET().build()
  return rest.loadJsonValue<List<GiteaPullRequestDTO>>(request).body()
}