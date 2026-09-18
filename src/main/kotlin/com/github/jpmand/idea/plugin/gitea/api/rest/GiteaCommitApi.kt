package com.github.jpmand.idea.plugin.gitea.api.rest

import com.github.jpmand.idea.plugin.gitea.api.GiteaApi
import com.github.jpmand.idea.plugin.gitea.api.rest.dto.Commit
import com.intellij.collaboration.api.json.loadJsonValue
import com.intellij.collaboration.util.resolveRelative

/**
 * Fetch a single repository commit, including the list of files it affected.
 *
 * @see <a href="https://gitea.com/api/swagger#/repository/repoGetSingleCommit">GET /repos/{owner}/{repo}/git/commits/{sha}</a>
 */
@Suppress("UnstableApiUsage")
suspend fun GiteaApi.repoGetSingleCommit(owner: String, repo: String, sha: String): Commit {
  val uri = server.restApiUri().resolveRelative("repos/$owner/$repo/git/commits/$sha")
  val request = request(uri).GET().build()
  return rest.loadJsonValue<Commit>(request).body()
}
