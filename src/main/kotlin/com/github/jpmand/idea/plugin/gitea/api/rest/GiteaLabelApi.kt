package com.github.jpmand.idea.plugin.gitea.api.rest

import com.github.jpmand.idea.plugin.gitea.api.GiteaApi
import com.github.jpmand.idea.plugin.gitea.api.GiteaUriUtil
import com.github.jpmand.idea.plugin.gitea.api.rest.dto.Label
import com.intellij.collaboration.api.json.loadJsonList
import com.intellij.collaboration.util.resolveRelative

/**
 * List a repository's labels.
 *
 * @see <a href="https://gitea.com/api/swagger#/issue/issueListLabels">GET /repos/{owner}/{repo}/labels</a>
 */
@Suppress("UnstableApiUsage")
suspend fun GiteaApi.repoListLabels(
  owner: String,
  repo: String,
  page: Int? = null,
  limit: Int? = null,
): List<Label> {
  val uri = GiteaUriUtil.QueryBuilder()
    .addParam("page", page)
    .addParam("limit", limit)
    .build(server.restApiUri().resolveRelative("repos/$owner/$repo/labels"))
  val request = request(uri).GET().build()
  return rest.loadJsonList<Label>(request).body()
}
