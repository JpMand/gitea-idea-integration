package com.github.jpmand.idea.plugin.gitea.api.rest

import com.github.jpmand.idea.plugin.gitea.api.GiteaApi
import com.github.jpmand.idea.plugin.gitea.api.GiteaUriUtil
import com.github.jpmand.idea.plugin.gitea.api.rest.dto.User
import com.intellij.collaboration.api.json.loadJsonList
import com.intellij.collaboration.util.resolveRelative

/**
 * List a repository's collaborators. Used to populate the PR-list "Author" filter with a stable
 * set of candidate authors (a low-privilege token may 403 here — callers should tolerate that).
 *
 * @see <a href="https://gitea.com/api/swagger#/repository/repoListCollaborators">GET /repos/{owner}/{repo}/collaborators</a>
 */
@Suppress("UnstableApiUsage")
suspend fun GiteaApi.repoListCollaborators(
  owner: String,
  repo: String,
  page: Int? = null,
  limit: Int? = null,
): List<User> {
  val uri = GiteaUriUtil.QueryBuilder()
    .addParam("page", page)
    .addParam("limit", limit)
    .build(server.restApiUri().resolveRelative("repos/$owner/$repo/collaborators"))
  val request = request(uri).GET().build()
  return rest.loadJsonList<User>(request).body()
}
