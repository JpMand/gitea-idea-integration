package com.github.jpmand.idea.plugin.gitea.pullrequest.data

import com.github.jpmand.idea.plugin.gitea.api.models.GiteaPullRequest
import com.github.jpmand.idea.plugin.gitea.api.models.GiteaReview
import com.github.jpmand.idea.plugin.gitea.api.models.GiteaReviewComment
import com.github.jpmand.idea.plugin.gitea.api.models.GiteaReviewThread
import com.github.jpmand.idea.plugin.gitea.api.models.toThreads
import com.github.jpmand.idea.plugin.gitea.api.rest.dto.CombinedStatus
import com.github.jpmand.idea.plugin.gitea.api.rest.dto.Commit
import com.github.jpmand.idea.plugin.gitea.api.rest.dto.CreatePullReviewOptions
import com.github.jpmand.idea.plugin.gitea.api.rest.dto.EditPullRequestOption
import com.github.jpmand.idea.plugin.gitea.api.rest.dto.MergePullRequestOption
import com.github.jpmand.idea.plugin.gitea.api.rest.decodeContent
import com.github.jpmand.idea.plugin.gitea.api.rest.getFileContents
import com.github.jpmand.idea.plugin.gitea.pullrequest.diff.GiteaPRChangedFile
import com.github.jpmand.idea.plugin.gitea.pullrequest.diff.toChangedFile
import com.github.jpmand.idea.plugin.gitea.api.rest.pr.repoCreatePullRequestReview
import com.github.jpmand.idea.plugin.gitea.api.rest.pr.repoEditPullRequest
import com.github.jpmand.idea.plugin.gitea.api.rest.pr.repoGetPullRequest
import com.github.jpmand.idea.plugin.gitea.api.rest.pr.repoGetPullRequestReviewComments
import com.github.jpmand.idea.plugin.gitea.api.rest.repoListCommitStatuses
import com.github.jpmand.idea.plugin.gitea.api.rest.pr.repoListPullRequestCommits
import com.github.jpmand.idea.plugin.gitea.api.rest.pr.repoListPullRequestFiles
import com.github.jpmand.idea.plugin.gitea.api.rest.pr.repoListPullRequestReviews
import com.github.jpmand.idea.plugin.gitea.api.rest.pr.repoListPullRequests
import com.github.jpmand.idea.plugin.gitea.api.rest.pr.repoMergePullRequest
import com.github.jpmand.idea.plugin.gitea.api.rest.pr.repoResolvePullRequestReviewComment
import com.github.jpmand.idea.plugin.gitea.api.rest.pr.repoUnresolvePullRequestReviewComment
import com.github.jpmand.idea.plugin.gitea.api.rest.repoCombinedStatus
import kotlinx.coroutines.CancellationException

/**
 * Data-access layer for PR operations scoped to a single [GiteaPRDataContext].
 *
 * Provides suspend functions that call the API and map responses to domain models.
 * Instantiate one per context; discard when the context changes.
 */
class GiteaPRRepository(private val ctx: GiteaPRDataContext) {

    private val owner: String get() = ctx.repo.repositoryPath.owner
    private val repo: String get() = ctx.repo.repositoryPath.repository

    // ── Pull Requests ─────────────────────────────────────────────────────

    suspend fun loadPullRequests(
        state: String? = "open",
        page: Int? = null,
        limit: Int? = null,
    ): List<GiteaPullRequest> =
        ctx.api.repoListPullRequests(owner, repo, null, state, null, null, null, null, page, limit)
            .map { GiteaPullRequest.fromDto(it) }

    suspend fun loadPullRequest(number: Int): GiteaPullRequest =
        GiteaPullRequest.fromDto(ctx.api.repoGetPullRequest(owner, repo, number))

    suspend fun editPullRequest(number: Int, body: EditPullRequestOption): GiteaPullRequest =
        GiteaPullRequest.fromDto(ctx.api.repoEditPullRequest(owner, repo, number, body))

    suspend fun mergePullRequest(number: Int, body: MergePullRequestOption) =
        ctx.api.repoMergePullRequest(owner, repo, number, body)

    // ── Reviews & Comments ────────────────────────────────────────────────

    suspend fun loadReviews(prNumber: Int): List<GiteaReview> =
        ctx.api.repoListPullRequestReviews(owner, repo, prNumber).map { GiteaReview.fromDto(it) }

    suspend fun loadReviewComments(prNumber: Int, reviewId: Long): List<GiteaReviewComment> =
        ctx.api.repoGetPullRequestReviewComments(owner, repo, prNumber, reviewId)
            .map { GiteaReviewComment.fromDto(it) }

    /** Convenience: load comments from all reviews in one call. */
    suspend fun loadAllReviewComments(prNumber: Int): List<GiteaReviewComment> =
        loadReviews(prNumber).flatMap { review -> loadReviewComments(prNumber, review.id) }

    /** Groups all review comments for a PR into synthetic [GiteaReviewThread]s. */
    suspend fun loadThreads(prNumber: Int): List<GiteaReviewThread> =
        loadAllReviewComments(prNumber).toThreads()

    suspend fun submitReview(prNumber: Int, body: CreatePullReviewOptions): GiteaReview =
        GiteaReview.fromDto(ctx.api.repoCreatePullRequestReview(owner, repo, prNumber, body))

    suspend fun resolveComment(commentId: Long): GiteaReviewComment =
        GiteaReviewComment.fromDto(ctx.api.repoResolvePullRequestReviewComment(owner, repo, commentId))

    suspend fun unresolveComment(commentId: Long): GiteaReviewComment =
        GiteaReviewComment.fromDto(ctx.api.repoUnresolvePullRequestReviewComment(owner, repo, commentId))

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

    suspend fun loadCommits(prNumber: Int): List<Commit> =
        ctx.api.repoListPullRequestCommits(owner, repo, prNumber)

    // ── CI Status ─────────────────────────────────────────────────────────

    suspend fun loadCombinedStatus(ref: String): CombinedStatus =
        ctx.api.repoCombinedStatus(owner, repo, ref)

    suspend fun loadCommitStatuses(ref: String) =
        ctx.api.repoListCommitStatuses(owner, repo, ref)
}
