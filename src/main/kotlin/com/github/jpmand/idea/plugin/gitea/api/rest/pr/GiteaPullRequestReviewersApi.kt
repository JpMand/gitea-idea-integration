package com.github.jpmand.idea.plugin.gitea.api.rest.pr

import com.github.jpmand.idea.plugin.gitea.api.GiteaApi
import com.github.jpmand.idea.plugin.gitea.api.rest.dto.PullReview
import com.github.jpmand.idea.plugin.gitea.api.rest.dto.PullReviewRequestOptions
import com.github.jpmand.idea.plugin.gitea.api.loadJsonList
import com.github.jpmand.idea.plugin.gitea.api.loadOptionalJsonValue
import com.intellij.collaboration.util.resolveRelative

/** POST /repos/{owner}/{repo}/pulls/{index}/requested_reviewers — request reviewers for a pull
 * request. */
@Suppress("UnstableApiUsage")
suspend fun GiteaApi.repoCreatePullReviewRequests(
  owner: String,
  repo: String,
  index: Int,
  body: PullReviewRequestOptions,
): List<PullReview> {
  val uri = server.restApiUri().resolveRelative("repos/$owner/$repo/pulls/$index/requested_reviewers")
  val request = rest.postJson(uri, body).build()
  return rest.loadJsonList<PullReview>(request).body()
}

/** DELETE /repos/{owner}/{repo}/pulls/{index}/requested_reviewers — cancel review requests for a
 * pull request. Gitea's own swagger spec documents this as returning 204 with an empty body on
 * success — loadOptionalJsonValue, not loadJsonValue. */
@Suppress("UnstableApiUsage")
suspend fun GiteaApi.repoDeletePullReviewRequests(
  owner: String,
  repo: String,
  index: Int,
  body: PullReviewRequestOptions,
) {
  val uri = server.restApiUri().resolveRelative("repos/$owner/$repo/pulls/$index/requested_reviewers")
  val request = rest.sendJson(uri, "DELETE", body).build()
  rest.loadOptionalJsonValue<Unit>(request)
}
