package com.github.jpmand.idea.plugin.gitea.api.rest

import com.github.jpmand.idea.plugin.gitea.api.GiteaApi
import com.github.jpmand.idea.plugin.gitea.api.GiteaUriUtil
import com.github.jpmand.idea.plugin.gitea.api.rest.models.GiteaStateEnum
import com.github.jpmand.idea.plugin.gitea.api.rest.models.commit.GiteaCommitDTO
import com.github.jpmand.idea.plugin.gitea.api.rest.models.pr.GiteaCreateIssueCommentDTO
import com.github.jpmand.idea.plugin.gitea.api.rest.models.pr.GiteaCreatePullRequestReviewRequestDTO
import com.github.jpmand.idea.plugin.gitea.api.rest.models.pr.GiteaDismissReviewRequestDTO
import com.github.jpmand.idea.plugin.gitea.api.rest.models.pr.GiteaEditIssueCommentDTO
import com.github.jpmand.idea.plugin.gitea.api.rest.models.pr.GiteaEditPullRequestRequestDTO
import com.github.jpmand.idea.plugin.gitea.api.rest.models.pr.GiteaIssueCommentDTO
import com.github.jpmand.idea.plugin.gitea.api.rest.models.pr.GiteaMergePullRequestRequestDTO
import com.github.jpmand.idea.plugin.gitea.api.rest.models.pr.GiteaPullRequestDTO
import com.github.jpmand.idea.plugin.gitea.api.rest.models.pr.GiteaPullRequestFileDTO
import com.github.jpmand.idea.plugin.gitea.api.rest.models.pr.GiteaPullRequestReviewCommentDTO
import com.github.jpmand.idea.plugin.gitea.api.rest.models.pr.GiteaPullRequestReviewDTO
import com.github.jpmand.idea.plugin.gitea.api.rest.models.pr.GiteaPullRequestSortEnum
import com.intellij.collaboration.api.json.loadJsonList
import com.intellij.collaboration.api.json.loadJsonValue
import com.intellij.collaboration.api.json.loadOptionalJsonValue
import com.intellij.collaboration.util.resolveRelative
import java.net.http.HttpRequest

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
  return rest.loadJsonList<GiteaPullRequestDTO>(request).body()
}

@Suppress("UnstableApiUsage")
suspend fun GiteaApi.repoListPinnedPullRequests(owner: String, repo: String): List<GiteaPullRequestDTO> {
  val uri = server.restApiUri().resolveRelative("repos/$owner/$repo/pulls/pinned")
  val request = request(uri).GET().build()
  return rest.loadJsonList<GiteaPullRequestDTO>(request).body()
}

// ── Single PR ─────────────────────────────────────────────────────────────

@Suppress("UnstableApiUsage")
suspend fun GiteaApi.repoGetPullRequest(owner: String, repo: String, index: Int): GiteaPullRequestDTO {
  val uri = server.restApiUri().resolveRelative("repos/$owner/$repo/pulls/$index")
  val request = request(uri).GET().build()
  return rest.loadJsonValue<GiteaPullRequestDTO>(request).body()
}

// ── Reviews ───────────────────────────────────────────────────────────────

@Suppress("UnstableApiUsage")
suspend fun GiteaApi.repoListPullRequestReviews(owner: String, repo: String, index: Int): List<GiteaPullRequestReviewDTO> {
  val uri = server.restApiUri().resolveRelative("repos/$owner/$repo/pulls/$index/reviews")
  val request = request(uri).GET().build()
  return rest.loadJsonList<GiteaPullRequestReviewDTO>(request).body()
}

@Suppress("UnstableApiUsage")
suspend fun GiteaApi.repoGetPullRequestReview(owner: String, repo: String, index: Int, id: Long): GiteaPullRequestReviewDTO {
  val uri = server.restApiUri().resolveRelative("repos/$owner/$repo/pulls/$index/reviews/$id")
  val request = request(uri).GET().build()
  return rest.loadJsonValue<GiteaPullRequestReviewDTO>(request).body()
}

@Suppress("UnstableApiUsage")
suspend fun GiteaApi.repoGetPullRequestReviewComments(
  owner: String,
  repo: String,
  index: Int,
  id: Long,
): List<GiteaPullRequestReviewCommentDTO> {
  val uri = server.restApiUri().resolveRelative("repos/$owner/$repo/pulls/$index/reviews/$id/comments")
  val request = request(uri).GET().build()
  return rest.loadJsonList<GiteaPullRequestReviewCommentDTO>(request).body()
}

@Suppress("UnstableApiUsage")
suspend fun GiteaApi.repoCreatePullRequestReview(
  owner: String,
  repo: String,
  index: Int,
  body: GiteaCreatePullRequestReviewRequestDTO,
): GiteaPullRequestReviewDTO {
  val uri = server.restApiUri().resolveRelative("repos/$owner/$repo/pulls/$index/reviews")
  val request = request(uri).POST(rest.jsonBodyPublisher(uri, body)).build()
  return rest.loadJsonValue<GiteaPullRequestReviewDTO>(request).body()
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
): GiteaPullRequestReviewDTO {
  val uri = server.restApiUri().resolveRelative("repos/$owner/$repo/pulls/$index/reviews/$id/dismissals")
  val body = GiteaDismissReviewRequestDTO(message)
  val request = request(uri).POST(rest.jsonBodyPublisher(uri, body)).build()
  return rest.loadJsonValue<GiteaPullRequestReviewDTO>(request).body()
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
): List<GiteaIssueCommentDTO> {
  val uri = GiteaUriUtil.QueryBuilder()
    .addParam("since", since)
    .addParam("before", before)
    .addParam("page", page)
    .addParam("limit", limit)
    .build(server.restApiUri().resolveRelative("repos/$owner/$repo/issues/$index/comments"))
  val request = request(uri).GET().build()
  return rest.loadJsonList<GiteaIssueCommentDTO>(request).body()
}

@Suppress("UnstableApiUsage")
suspend fun GiteaApi.repoCreatePullRequestComment(
  owner: String,
  repo: String,
  index: Int,
  body: GiteaCreateIssueCommentDTO,
): GiteaIssueCommentDTO {
  val uri = server.restApiUri().resolveRelative("repos/$owner/$repo/issues/$index/comments")
  val request = request(uri).POST(rest.jsonBodyPublisher(uri, body)).build()
  return rest.loadJsonValue<GiteaIssueCommentDTO>(request).body()
}

@Suppress("UnstableApiUsage")
suspend fun GiteaApi.repoEditPullRequestComment(
  owner: String,
  repo: String,
  commentId: Long,
  body: GiteaEditIssueCommentDTO,
): GiteaIssueCommentDTO {
  val uri = server.restApiUri().resolveRelative("repos/$owner/$repo/issues/comments/$commentId")
  val request = request(uri).method("PATCH", rest.jsonBodyPublisher(uri, body)).build()
  return rest.loadJsonValue<GiteaIssueCommentDTO>(request).body()
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
): List<GiteaPullRequestFileDTO> {
  val uri = GiteaUriUtil.QueryBuilder()
    .addParam("page", page)
    .addParam("limit", limit)
    .build(server.restApiUri().resolveRelative("repos/$owner/$repo/pulls/$index/files"))
  val request = request(uri).GET().build()
  return rest.loadJsonList<GiteaPullRequestFileDTO>(request).body()
}

@Suppress("UnstableApiUsage")
suspend fun GiteaApi.repoListPullRequestCommits(
  owner: String,
  repo: String,
  index: Int,
  page: Int? = null,
  limit: Int? = null,
): List<GiteaCommitDTO> {
  val uri = GiteaUriUtil.QueryBuilder()
    .addParam("page", page)
    .addParam("limit", limit)
    .build(server.restApiUri().resolveRelative("repos/$owner/$repo/pulls/$index/commits"))
  val request = request(uri).GET().build()
  return rest.loadJsonList<GiteaCommitDTO>(request).body()
}

// ── Edit / Merge ──────────────────────────────────────────────────────────

/** PATCH /repos/{owner}/{repo}/pulls/{index} — edit title, body, state, assignees, etc. */
@Suppress("UnstableApiUsage")
suspend fun GiteaApi.repoEditPullRequest(
  owner: String,
  repo: String,
  index: Int,
  body: GiteaEditPullRequestRequestDTO,
): GiteaPullRequestDTO {
  val uri = server.restApiUri().resolveRelative("repos/$owner/$repo/pulls/$index")
  val request = request(uri).method("PATCH", rest.jsonBodyPublisher(uri, body)).build()
  return rest.loadJsonValue<GiteaPullRequestDTO>(request).body()
}

/** POST /repos/{owner}/{repo}/pulls/{index}/merge — merge the pull request. */
@Suppress("UnstableApiUsage")
suspend fun GiteaApi.repoMergePullRequest(
  owner: String,
  repo: String,
  index: Int,
  body: GiteaMergePullRequestRequestDTO,
) {
  val uri = server.restApiUri().resolveRelative("repos/$owner/$repo/pulls/$index/merge")
  val request = request(uri).POST(rest.jsonBodyPublisher(uri, body)).build()
  rest.loadOptionalJsonValue<Unit>(request)
}

// ── Comment resolve / unresolve ───────────────────────────────────────────

/** POST /repos/{owner}/{repo}/pulls/comments/{id}/resolve — mark comment as resolved. */
@Suppress("UnstableApiUsage")
suspend fun GiteaApi.repoResolvePullRequestReviewComment(
  owner: String,
  repo: String,
  commentId: Long,
): GiteaPullRequestReviewCommentDTO {
  val uri = server.restApiUri().resolveRelative("repos/$owner/$repo/pulls/comments/$commentId/resolve")
  val request = request(uri).POST(HttpRequest.BodyPublishers.noBody()).build()
  return rest.loadJsonValue<GiteaPullRequestReviewCommentDTO>(request).body()
}

/** POST /repos/{owner}/{repo}/pulls/comments/{id}/unresolve — un-resolve a resolved comment. */
@Suppress("UnstableApiUsage")
suspend fun GiteaApi.repoUnresolvePullRequestReviewComment(
  owner: String,
  repo: String,
  commentId: Long,
): GiteaPullRequestReviewCommentDTO {
  val uri = server.restApiUri().resolveRelative("repos/$owner/$repo/pulls/comments/$commentId/unresolve")
  val request = request(uri).POST(HttpRequest.BodyPublishers.noBody()).build()
  return rest.loadJsonValue<GiteaPullRequestReviewCommentDTO>(request).body()
}

