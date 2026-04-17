package com.github.jpmand.idea.plugin.gitea.pullrequest.data

import com.github.jpmand.idea.plugin.gitea.api.models.GiteaPullRequest
import com.github.jpmand.idea.plugin.gitea.api.models.GiteaReview
import com.github.jpmand.idea.plugin.gitea.api.models.GiteaReviewComment
import com.github.jpmand.idea.plugin.gitea.api.rest.models.GiteaCombinedStatusDTO
import com.github.jpmand.idea.plugin.gitea.api.rest.models.GiteaStateEnum
import com.github.jpmand.idea.plugin.gitea.api.rest.models.commit.GiteaCommitDTO
import com.github.jpmand.idea.plugin.gitea.api.rest.models.pr.GiteaCreatePullRequestReviewRequestDTO
import com.github.jpmand.idea.plugin.gitea.api.rest.models.pr.GiteaEditPullRequestRequestDTO
import com.github.jpmand.idea.plugin.gitea.api.rest.models.pr.GiteaMergePullRequestRequestDTO
import com.github.jpmand.idea.plugin.gitea.api.rest.getFileContents
import com.github.jpmand.idea.plugin.gitea.pullrequest.diff.GiteaPRChangedFile
import com.github.jpmand.idea.plugin.gitea.pullrequest.diff.toChangedFile
import com.github.jpmand.idea.plugin.gitea.api.rest.repoCreatePullRequestReview
import com.github.jpmand.idea.plugin.gitea.api.rest.repoEditPullRequest
import com.github.jpmand.idea.plugin.gitea.api.rest.repoGetPullRequest
import com.github.jpmand.idea.plugin.gitea.api.rest.repoGetPullRequestReviewComments
import com.github.jpmand.idea.plugin.gitea.api.rest.repoListCommitStatuses
import com.github.jpmand.idea.plugin.gitea.api.rest.repoListPullRequestCommits
import com.github.jpmand.idea.plugin.gitea.api.rest.repoListPullRequestFiles
import com.github.jpmand.idea.plugin.gitea.api.rest.repoListPullRequestReviews
import com.github.jpmand.idea.plugin.gitea.api.rest.repoListPullRequests
import com.github.jpmand.idea.plugin.gitea.api.rest.repoMergePullRequest
import com.github.jpmand.idea.plugin.gitea.api.rest.repoResolvePullRequestReviewComment
import com.github.jpmand.idea.plugin.gitea.api.rest.repoUnresolvePullRequestReviewComment
import com.github.jpmand.idea.plugin.gitea.api.rest.repoCombinedStatus
import kotlinx.coroutines.CancellationException

/**
 * Data-access layer for PR operations scoped to a single [GiteaPRDataContext].
 *
 * Provides suspend functions that call the API and map responses to domain models.
 * Instantiate one per context; discard when the context changes.
 */
@Suppress("UnstableApiUsage")
class GiteaPRRepository(private val ctx: GiteaPRDataContext) {

    private val owner: String get() = ctx.repo.repositoryPath.owner
    private val repo: String get() = ctx.repo.repositoryPath.repository

    // ── Pull Requests ─────────────────────────────────────────────────────

    suspend fun loadPullRequests(
        state: GiteaStateEnum? = GiteaStateEnum.OPEN,
        page: Int? = null,
        limit: Int? = null,
    ): List<GiteaPullRequest> =
        ctx.api.repoListPullRequests(owner, repo, null, state, null, null, null, null, page, limit)
            .map { it.toPullRequest() }

    suspend fun loadPullRequest(number: Int): GiteaPullRequest =
        ctx.api.repoGetPullRequest(owner, repo, number).toPullRequest()

    suspend fun editPullRequest(number: Int, body: GiteaEditPullRequestRequestDTO): GiteaPullRequest =
        ctx.api.repoEditPullRequest(owner, repo, number, body).toPullRequest()

    suspend fun mergePullRequest(number: Int, body: GiteaMergePullRequestRequestDTO) =
        ctx.api.repoMergePullRequest(owner, repo, number, body)

    // ── Reviews & Comments ────────────────────────────────────────────────

    suspend fun loadReviews(prNumber: Int): List<GiteaReview> =
        ctx.api.repoListPullRequestReviews(owner, repo, prNumber).map { it.toReview() }

    suspend fun loadReviewComments(prNumber: Int, reviewId: Long): List<GiteaReviewComment> =
        ctx.api.repoGetPullRequestReviewComments(owner, repo, prNumber, reviewId)
            .map { it.toReviewComment() }

    /** Convenience: load comments from all reviews in one call. */
    suspend fun loadAllReviewComments(prNumber: Int): List<GiteaReviewComment> =
        loadReviews(prNumber).flatMap { review -> loadReviewComments(prNumber, review.id) }

    suspend fun submitReview(prNumber: Int, body: GiteaCreatePullRequestReviewRequestDTO): GiteaReview =
        ctx.api.repoCreatePullRequestReview(owner, repo, prNumber, body).toReview()

    suspend fun resolveComment(commentId: Long): GiteaReviewComment =
        ctx.api.repoResolvePullRequestReviewComment(owner, repo, commentId).toReviewComment()

    suspend fun unresolveComment(commentId: Long): GiteaReviewComment =
        ctx.api.repoUnresolvePullRequestReviewComment(owner, repo, commentId).toReviewComment()

    // ── Files & Commits ───────────────────────────────────────────────────

    /** Returns domain models for files changed in the given PR. */
    suspend fun loadChangedFiles(prNumber: Int): List<GiteaPRChangedFile> =
        ctx.api.repoListPullRequestFiles(owner, repo, prNumber).map { it.toChangedFile() }

    /**
     * Fetches the raw text content of a file at a specific ref (branch name, tag, or SHA).
     * Returns an empty string when the file does not exist at that ref (e.g. for added/deleted files).
     */
    suspend fun loadFileContent(path: String, ref: String): String {
        return try {
            val dto = ctx.api.getFileContents(owner, repo, path, ref)
            dto.decodeContent() ?: ""
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            ""
        }
    }

    suspend fun loadCommits(prNumber: Int): List<GiteaCommitDTO> =
        ctx.api.repoListPullRequestCommits(owner, repo, prNumber)

    // ── CI Status ─────────────────────────────────────────────────────────

    suspend fun loadCombinedStatus(ref: String): GiteaCombinedStatusDTO =
        ctx.api.repoCombinedStatus(owner, repo, ref)

    suspend fun loadCommitStatuses(ref: String) =
        ctx.api.repoListCommitStatuses(owner, repo, ref)
}
