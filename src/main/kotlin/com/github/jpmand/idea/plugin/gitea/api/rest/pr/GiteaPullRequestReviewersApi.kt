package com.github.jpmand.idea.plugin.gitea.api.rest.pr

import com.github.jpmand.idea.plugin.gitea.api.GiteaApi
import com.github.jpmand.idea.plugin.gitea.api.rest.dto.PullReview
import com.github.jpmand.idea.plugin.gitea.api.rest.dto.PullReviewRequestOptions
import com.intellij.collaboration.api.httpclient.HttpClientUtil
import com.intellij.collaboration.api.json.loadJsonList
import com.intellij.collaboration.api.json.loadOptionalJsonValue
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
  val request = request(uri).POST(rest.jsonBodyPublisher(uri, body))
    .setHeader(HttpClientUtil.CONTENT_TYPE_HEADER, HttpClientUtil.CONTENT_TYPE_JSON)
    .build()
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
  val request = request(uri).method("DELETE", rest.jsonBodyPublisher(uri, body))
    .setHeader(HttpClientUtil.CONTENT_TYPE_HEADER, HttpClientUtil.CONTENT_TYPE_JSON)
    .build()
  rest.loadOptionalJsonValue<Unit>(request)
}
