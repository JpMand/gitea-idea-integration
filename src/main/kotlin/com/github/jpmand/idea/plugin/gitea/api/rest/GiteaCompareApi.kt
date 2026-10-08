package com.github.jpmand.idea.plugin.gitea.api.rest

import com.github.jpmand.idea.plugin.gitea.api.GiteaApi
import com.github.jpmand.idea.plugin.gitea.api.GiteaUriUtil
import com.intellij.collaboration.api.httpclient.HttpClientUtil
import com.intellij.collaboration.util.resolveRelative
import com.intellij.openapi.diagnostic.fileLogger

private val LOG = fileLogger()

/**
 * The raw unified diff (`git diff` output) from [base] to [head] — Gitea's JSON comparison lists
 * the commits in between, not the changed files.
 *
 * @see <a href="https://gitea.com/api/swagger#/repository/repoCompareDiff">GET /repos/{owner}/{repo}/compare/{basehead}?output=diff</a>
 */
@Suppress("UnstableApiUsage")
suspend fun GiteaApi.repoCompareDiff(owner: String, repo: String, base: String, head: String): String {
  val baseUri = server.restApiUri().resolveRelative("repos/$owner/$repo/compare/$base...$head")
  val uri = GiteaUriUtil.QueryBuilder().addParam("output", "diff").build(baseUri)
  val request = request(uri).GET().build()
  val bodyHandler = HttpClientUtil.inflateAndReadWithErrorHandlingAndLogging(LOG, request) { reader, _ -> reader.readText() }
  return sendAndAwaitCancellable(request, bodyHandler).body()
}
