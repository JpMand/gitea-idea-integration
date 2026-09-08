package com.github.jpmand.idea.plugin.gitea.api.rest.pr

import com.github.jpmand.idea.plugin.gitea.api.GiteaApi
import com.github.jpmand.idea.plugin.gitea.api.GiteaUriUtil
import com.github.jpmand.idea.plugin.gitea.api.rest.dto.ChangedFile
import com.github.jpmand.idea.plugin.gitea.api.rest.dto.Comment
import com.github.jpmand.idea.plugin.gitea.api.rest.dto.Commit
import com.github.jpmand.idea.plugin.gitea.api.rest.dto.CreateIssueCommentOption
import com.github.jpmand.idea.plugin.gitea.api.rest.dto.CreatePullReviewOptions
import com.github.jpmand.idea.plugin.gitea.api.rest.dto.DismissPullReviewOptions
import com.github.jpmand.idea.plugin.gitea.api.rest.dto.EditIssueCommentOption
import com.github.jpmand.idea.plugin.gitea.api.rest.dto.EditPullRequestOption
import com.github.jpmand.idea.plugin.gitea.api.rest.dto.MergePullRequestOption
import com.github.jpmand.idea.plugin.gitea.api.rest.dto.PullRequest
import com.github.jpmand.idea.plugin.gitea.api.rest.dto.PullReview
import com.github.jpmand.idea.plugin.gitea.api.rest.dto.PullReviewComment
import com.github.jpmand.idea.plugin.gitea.api.rest.dto.TimelineComment
import com.intellij.collaboration.api.httpclient.HttpClientUtil
import com.intellij.collaboration.api.json.loadJsonList
import com.intellij.collaboration.api.json.loadJsonValue
import com.intellij.collaboration.api.json.loadOptionalJsonValue
import com.intellij.collaboration.util.resolveRelative
import java.net.http.HttpRequest

/**
 * List a repo's pull requests
 *
 * @param owner Owner of the repo
 * @param repo Name of the repo
 * @param baseBranch Filter by target base branch of the pull request
 * @param state State of pull request ("open", "closed", or "all" — Gitea's list-filter query
 *   param accepts "all" even though the response object's own `state` field only has open/closed)
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
  state: String?,
  sort: GiteaPullRequestSortEnum?,
  milestone: Int?,
  labels: List<String>?,
  poster: String?,
  page: Int?,
  limit: Int?,
): List<PullRequest> {
  val uri = GiteaUriUtil.QueryBuilder()
    .addParam("base_branch", baseBranch)
    .addParam("state", state)
    .addParam("sort", sort?.value)
    .addParam("milestone", milestone)
    .addParam("labels", labels?.joinToString(","))
    .addParam("poster", poster)
    .addParam("page", page)
    .addParam("limit", limit)
    .build(server.restApiUri().resolveRelative("repos/$owner/$repo/pulls"))
  val request = request(uri).GET().build()
  return rest.loadJsonList<PullRequest>(request).body()
}

@Suppress("UnstableApiUsage")
suspend fun GiteaApi.repoListPinnedPullRequests(owner: String, repo: String): List<PullRequest> {
  val uri = server.restApiUri().resolveRelative("repos/$owner/$repo/pulls/pinned")
  val request = request(uri).GET().build()
  return rest.loadJsonList<PullRequest>(request).body()
}

// ── Single PR ─────────────────────────────────────────────────────────────

@Suppress("UnstableApiUsage")
suspend fun GiteaApi.repoGetPullRequest(owner: String, repo: String, index: Int): PullRequest {
  val uri = server.restApiUri().resolveRelative("repos/$owner/$repo/pulls/$index")
  val request = request(uri).GET().build()
  return rest.loadJsonValue<PullRequest>(request).body()
}

// ── Reviews ───────────────────────────────────────────────────────────────

@Suppress("UnstableApiUsage")
suspend fun GiteaApi.repoListPullRequestReviews(owner: String, repo: String, index: Int): List<PullReview> {
  val uri = server.restApiUri().resolveRelative("repos/$owner/$repo/pulls/$index/reviews")
  val request = request(uri).GET().build()
  return rest.loadJsonList<PullReview>(request).body()
}

@Suppress("UnstableApiUsage")
suspend fun GiteaApi.repoGetPullRequestReview(owner: String, repo: String, index: Int, id: Long): PullReview {
  val uri = server.restApiUri().resolveRelative("repos/$owner/$repo/pulls/$index/reviews/$id")
  val request = request(uri).GET().build()
  return rest.loadJsonValue<PullReview>(request).body()
}

@Suppress("UnstableApiUsage")
suspend fun GiteaApi.repoGetPullRequestReviewComments(
  owner: String,
  repo: String,
  index: Int,
  id: Long,
): List<PullReviewComment> {
  val uri = server.restApiUri().resolveRelative("repos/$owner/$repo/pulls/$index/reviews/$id/comments")
  val request = request(uri).GET().build()
  return rest.loadJsonList<PullReviewComment>(request).body()
}

@Suppress("UnstableApiUsage")
suspend fun GiteaApi.repoCreatePullRequestReview(
  owner: String,
  repo: String,
  index: Int,
  body: CreatePullReviewOptions,
): PullReview {
  val uri = server.restApiUri().resolveRelative("repos/$owner/$repo/pulls/$index/reviews")
  val request = request(uri).POST(rest.jsonBodyPublisher(uri, body))
    .setHeader(HttpClientUtil.CONTENT_TYPE_HEADER, HttpClientUtil.CONTENT_TYPE_JSON)
    .build()
  return rest.loadJsonValue<PullReview>(request).body()
}

@Suppress("UnstableApiUsage")
suspend fun GiteaApi.repoDeletePullRequestReview(owner: String, repo: String, index: Int, id: Long) {
  val uri = server.restApiUri().resolveRelative("repos/$owner/$repo/pulls/$index/reviews/$id")
  val request = request(uri).DELETE().build()
  rest.loadOptionalJsonValue<Unit>(request)
}

@Suppress("UnstableApiUsage")
suspend fun GiteaApi.repoDismissPullRequestReview(
  owner: String,
  repo: String,
  index: Int,
  id: Long,
  message: String,
): PullReview {
  val uri = server.restApiUri().resolveRelative("repos/$owner/$repo/pulls/$index/reviews/$id/dismissals")
  val body = DismissPullReviewOptions(message)
  val request = request(uri).POST(rest.jsonBodyPublisher(uri, body))
    .setHeader(HttpClientUtil.CONTENT_TYPE_HEADER, HttpClientUtil.CONTENT_TYPE_JSON)
    .build()
  return rest.loadJsonValue<PullReview>(request).body()
}

// ── Issue/PR comments ─────────────────────────────────────────────────────

@Suppress("UnstableApiUsage")
suspend fun GiteaApi.repoListPullRequestComments(
  owner: String,
  repo: String,
  index: Int,
  since: String? = null,
  before: String? = null,
  page: Int? = null,
  limit: Int? = null,
): List<Comment> {
  val uri = GiteaUriUtil.QueryBuilder()
    .addParam("since", since)
    .addParam("before", before)
    .addParam("page", page)
    .addParam("limit", limit)
    .build(server.restApiUri().resolveRelative("repos/$owner/$repo/issues/$index/comments"))
  val request = request(uri).GET().build()
  return rest.loadJsonList<Comment>(request).body()
}

/**
 * List every comment AND event on a PR/issue in chronological order.
 *
 * `GET /repos/{owner}/{repo}/issues/{index}/timeline` (`issueGetCommentsAndTimeline`).
 */
@Suppress("UnstableApiUsage")
suspend fun GiteaApi.issueListTimeline(
  owner: String,
  repo: String,
  index: Int,
  since: String? = null,
  before: String? = null,
  page: Int? = null,
  limit: Int? = null,
): List<TimelineComment> {
  val uri = GiteaUriUtil.QueryBuilder()
    .addParam("since", since)
    .addParam("before", before)
    .addParam("page", page)
    .addParam("limit", limit)
    .build(server.restApiUri().resolveRelative("repos/$owner/$repo/issues/$index/timeline"))
  val request = request(uri).GET().build()
  return rest.loadJsonList<TimelineComment>(request).body()
}

@Suppress("UnstableApiUsage")
suspend fun GiteaApi.repoCreatePullRequestComment(
  owner: String,
  repo: String,
  index: Int,
  body: CreateIssueCommentOption,
): Comment {
  val uri = server.restApiUri().resolveRelative("repos/$owner/$repo/issues/$index/comments")
  val request = request(uri).POST(rest.jsonBodyPublisher(uri, body))
    .setHeader(HttpClientUtil.CONTENT_TYPE_HEADER, HttpClientUtil.CONTENT_TYPE_JSON)
    .build()
  return rest.loadJsonValue<Comment>(request).body()
}

@Suppress("UnstableApiUsage")
suspend fun GiteaApi.repoEditPullRequestComment(
  owner: String,
  repo: String,
  commentId: Long,
  body: EditIssueCommentOption,
): Comment {
  val uri = server.restApiUri().resolveRelative("repos/$owner/$repo/issues/comments/$commentId")
  val request = request(uri).method("PATCH", rest.jsonBodyPublisher(uri, body))
    .setHeader(HttpClientUtil.CONTENT_TYPE_HEADER, HttpClientUtil.CONTENT_TYPE_JSON)
    .build()
  return rest.loadJsonValue<Comment>(request).body()
}

@Suppress("UnstableApiUsage")
suspend fun GiteaApi.repoDeletePullRequestComment(owner: String, repo: String, commentId: Long) {
  val uri = server.restApiUri().resolveRelative("repos/$owner/$repo/issues/comments/$commentId")
  val request = request(uri).DELETE().build()
  rest.loadOptionalJsonValue<Unit>(request)
}

// ── Files and commits ─────────────────────────────────────────────────────

@Suppress("UnstableApiUsage")
suspend fun GiteaApi.repoListPullRequestFiles(
  owner: String,
  repo: String,
  index: Int,
  page: Int? = null,
  limit: Int? = null,
): List<ChangedFile> {
  val uri = GiteaUriUtil.QueryBuilder()
    .addParam("page", page)
    .addParam("limit", limit)
    .build(server.restApiUri().resolveRelative("repos/$owner/$repo/pulls/$index/files"))
  val request = request(uri).GET().build()
  return rest.loadJsonList<ChangedFile>(request).body()
}

@Suppress("UnstableApiUsage")
suspend fun GiteaApi.repoListPullRequestCommits(
  owner: String,
  repo: String,
  index: Int,
  page: Int? = null,
  limit: Int? = null,
): List<Commit> {
  val uri = GiteaUriUtil.QueryBuilder()
    .addParam("page", page)
    .addParam("limit", limit)
    .build(server.restApiUri().resolveRelative("repos/$owner/$repo/pulls/$index/commits"))
  val request = request(uri).GET().build()
  return rest.loadJsonList<Commit>(request).body()
}

// ── Edit / Merge ──────────────────────────────────────────────────────────

/** PATCH /repos/{owner}/{repo}/pulls/{index} — edit title, body, state, assignees, etc. */
@Suppress("UnstableApiUsage")
suspend fun GiteaApi.repoEditPullRequest(
  owner: String,
  repo: String,
  index: Int,
  body: EditPullRequestOption,
): PullRequest {
  val uri = server.restApiUri().resolveRelative("repos/$owner/$repo/pulls/$index")
  val request = request(uri).method("PATCH", rest.jsonBodyPublisher(uri, body))
    .setHeader(HttpClientUtil.CONTENT_TYPE_HEADER, HttpClientUtil.CONTENT_TYPE_JSON)
    .build()
  return rest.loadJsonValue<PullRequest>(request).body()
}

/** POST /repos/{owner}/{repo}/pulls/{index}/merge — merge the pull request. */
@Suppress("UnstableApiUsage")
suspend fun GiteaApi.repoMergePullRequest(
  owner: String,
  repo: String,
  index: Int,
  body: MergePullRequestOption,
) {
  val uri = server.restApiUri().resolveRelative("repos/$owner/$repo/pulls/$index/merge")
  val request = request(uri).POST(rest.jsonBodyPublisher(uri, body))
    .setHeader(HttpClientUtil.CONTENT_TYPE_HEADER, HttpClientUtil.CONTENT_TYPE_JSON)
    .build()
  rest.loadOptionalJsonValue<Unit>(request)
}

// ── Comment resolve / unresolve ───────────────────────────────────────────

/** POST /repos/{owner}/{repo}/pulls/comments/{id}/resolve — mark comment as resolved. */
@Suppress("UnstableApiUsage")
suspend fun GiteaApi.repoResolvePullRequestReviewComment(
  owner: String,
  repo: String,
  commentId: Long,
): PullReviewComment {
  val uri = server.restApiUri().resolveRelative("repos/$owner/$repo/pulls/comments/$commentId/resolve")
  val request = request(uri).POST(HttpRequest.BodyPublishers.noBody()).build()
  return rest.loadJsonValue<PullReviewComment>(request).body()
}

/** POST /repos/{owner}/{repo}/pulls/comments/{id}/unresolve — un-resolve a resolved comment. */
@Suppress("UnstableApiUsage")
suspend fun GiteaApi.repoUnresolvePullRequestReviewComment(
  owner: String,
  repo: String,
  commentId: Long,
): PullReviewComment {
  val uri = server.restApiUri().resolveRelative("repos/$owner/$repo/pulls/comments/$commentId/unresolve")
  val request = request(uri).POST(HttpRequest.BodyPublishers.noBody()).build()
  return rest.loadJsonValue<PullReviewComment>(request).body()
}
