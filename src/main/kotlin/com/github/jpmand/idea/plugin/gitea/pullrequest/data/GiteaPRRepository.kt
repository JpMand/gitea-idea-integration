package com.github.jpmand.idea.plugin.gitea.pullrequest.data

import com.github.jpmand.idea.plugin.gitea.api.models.GiteaCommit
import com.github.jpmand.idea.plugin.gitea.api.models.GiteaCommitStatus
import com.github.jpmand.idea.plugin.gitea.api.models.GiteaLabel
import com.github.jpmand.idea.plugin.gitea.api.models.GiteaPullRequest
import com.github.jpmand.idea.plugin.gitea.api.models.GiteaReview
import com.github.jpmand.idea.plugin.gitea.api.models.GiteaUser
import com.github.jpmand.idea.plugin.gitea.api.models.GiteaReviewComment
import com.github.jpmand.idea.plugin.gitea.api.models.GiteaReviewState
import com.github.jpmand.idea.plugin.gitea.api.models.GiteaReviewThread
import com.github.jpmand.idea.plugin.gitea.api.models.GiteaTimelineItem
import com.github.jpmand.idea.plugin.gitea.api.models.toDate
import com.github.jpmand.idea.plugin.gitea.api.models.toThreads
import com.github.jpmand.idea.plugin.gitea.api.models.toTimelineItemOrNull
import com.github.jpmand.idea.plugin.gitea.api.rest.dto.TimelineComment
import com.github.jpmand.idea.plugin.gitea.api.rest.pr.issueListTimeline
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
import com.github.jpmand.idea.plugin.gitea.api.rest.pr.repoListPullRequestCommits
import com.github.jpmand.idea.plugin.gitea.api.rest.pr.repoListPullRequestFiles
import com.github.jpmand.idea.plugin.gitea.api.rest.pr.repoListPullRequestReviews
import com.github.jpmand.idea.plugin.gitea.api.rest.pr.repoListPullRequests
import com.github.jpmand.idea.plugin.gitea.api.rest.pr.repoMergePullRequest
import com.github.jpmand.idea.plugin.gitea.api.rest.pr.repoResolvePullRequestReviewComment
import com.github.jpmand.idea.plugin.gitea.api.rest.pr.repoUnresolvePullRequestReviewComment
import com.github.jpmand.idea.plugin.gitea.api.rest.pr.GiteaPullRequestSortEnum
import com.github.jpmand.idea.plugin.gitea.api.rest.repoCombinedStatus
import com.github.jpmand.idea.plugin.gitea.api.rest.repoGetSingleCommit
import com.github.jpmand.idea.plugin.gitea.api.rest.repoListCollaborators
import com.github.jpmand.idea.plugin.gitea.api.GITEA_PAGE_SIZE
import com.github.jpmand.idea.plugin.gitea.api.GiteaRepositoryCoordinates
import com.github.jpmand.idea.plugin.gitea.api.giteaApiCall
import com.github.jpmand.idea.plugin.gitea.api.loadAllGiteaPages
import com.github.jpmand.idea.plugin.gitea.api.rest.repoListLabels
import com.intellij.collaboration.api.HttpStatusErrorException
import com.intellij.openapi.components.service

/**
 * Data-access layer for PR operations scoped to a single [GiteaPRDataContext].
 *
 * Provides suspend functions that call the API and map responses to domain models.
 * Instantiate one per context; discard when the context changes.
 */
class GiteaPRRepository(private val ctx: GiteaPRDataContext) {

    private val owner: String get() = ctx.repo.repositoryPath.owner
    private val repo: String get() = ctx.repo.repositoryPath.repository

    /** The Gitea repo this PR belongs to — used to resolve the matching git4idea repository/remote. */
    val repositoryCoordinates: GiteaRepositoryCoordinates get() = ctx.repo

    // ── Pull Requests ─────────────────────────────────────────────────────

    suspend fun loadPullRequests(
        state: String? = "open",
        sort: GiteaPullRequestSortEnum? = null,
        labels: List<String>? = null,
        poster: String? = null,
        page: Int? = null,
        limit: Int? = null,
    ): List<GiteaPullRequest> = giteaApiCall {
        ctx.api.repoListPullRequests(owner, repo, null, state, sort, null, labels, poster, page, limit)
            .map { GiteaPullRequest.fromDto(it) }
    }

    /** Repository labels, for the PR-list "Label" filter. */
    suspend fun loadLabels(): List<GiteaLabel> =
        loadAllGiteaPages { page -> ctx.api.repoListLabels(owner, repo, page = page, limit = GITEA_PAGE_SIZE) }
            .map { GiteaLabel.fromDto(it) }

    /**
     * Candidate PR authors for the "Author" filter — the repo's collaborators. Returns an empty
     * list (rather than throwing) only on 403, i.e. when the token lacks permission to enumerate
     * collaborators; any other failure propagates.
     */
    suspend fun loadPossibleAuthors(): List<GiteaUser> =
        try {
            ctx.api.repoListCollaborators(owner, repo, page = null, limit = 100).map { GiteaUser.fromDto(it) }
        } catch (e: HttpStatusErrorException) {
            if (e.statusCode == 403) emptyList() else throw e
        }

    suspend fun loadPullRequest(number: Int): GiteaPullRequest = giteaApiCall {
        GiteaPullRequest.fromDto(ctx.api.repoGetPullRequest(owner, repo, number))
    }

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

    /**
     * The PR's full activity timeline (Conversation): comments, commits, submitted reviews (with
     * their inline threads), and metadata events, in chronological order.
     */
    suspend fun loadTimeline(prNumber: Int): List<GiteaTimelineItem> {
        val timeline = loadAllGiteaPages { page ->
            ctx.api.issueListTimeline(owner, repo, prNumber, page = page, limit = GITEA_PAGE_SIZE)
        }
        val reviewsById = loadReviews(prNumber).associateBy { it.id }
        val threadsByReviewId = loadAllReviewComments(prNumber)
            .groupBy { it.reviewId ?: 0L }
            .mapValues { (_, comments) -> comments.toThreads() }
        val commits = loadCommits(prNumber)
        return mergeTimeline(timeline, reviewsById, threadsByReviewId, commits)
    }

    suspend fun submitReview(prNumber: Int, body: CreatePullReviewOptions): GiteaReview =
        GiteaReview.fromDto(ctx.api.repoCreatePullRequestReview(owner, repo, prNumber, body))

    suspend fun resolveComment(commentId: Long): GiteaReviewComment =
        GiteaReviewComment.fromDto(ctx.api.repoResolvePullRequestReviewComment(owner, repo, commentId))

    suspend fun unresolveComment(commentId: Long): GiteaReviewComment =
        GiteaReviewComment.fromDto(ctx.api.repoUnresolvePullRequestReviewComment(owner, repo, commentId))

    // ── Files & Commits ───────────────────────────────────────────────────

    /** Returns domain models for files changed in the given PR (base..head). */
    suspend fun loadChangedFiles(prNumber: Int): List<GiteaPRChangedFile> =
        loadAllGiteaPages { page -> ctx.api.repoListPullRequestFiles(owner, repo, prNumber, page = page, limit = GITEA_PAGE_SIZE) }
            .map { it.toChangedFile() }

    /** Returns domain models for files changed by a single commit. */
    suspend fun loadCommitChangedFiles(sha: String): List<GiteaPRChangedFile> =
        ctx.api.repoGetSingleCommit(owner, repo, sha).files.orEmpty().map { it.toChangedFile() }

    /**
     * Fetches the raw text content of a file at a specific ref (branch name, tag, or SHA).
     * Returns an empty string only when the file genuinely does not exist at that ref (404 — the
     * normal case for the base side of an added file or the head side of a deleted one). Any other
     * failure propagates, so a transient error is not silently rendered as a whole-file add/delete.
     */
    suspend fun loadFileContent(path: String, ref: String): String =
        try {
            ctx.api.getFileContents(owner, repo, path, ref).decodeContent() ?: ""
        } catch (e: HttpStatusErrorException) {
            if (e.statusCode == 404) "" else throw e
        }

    suspend fun loadCommits(prNumber: Int): List<GiteaCommit> =
        loadAllGiteaPages { page -> ctx.api.repoListPullRequestCommits(owner, repo, prNumber, page = page, limit = GITEA_PAGE_SIZE) }
            .map { GiteaCommit.fromDto(it) }

    // ── CI Status ─────────────────────────────────────────────────────────

    suspend fun loadCombinedStatus(ref: String): List<GiteaCommitStatus> =
        ctx.api.repoCombinedStatus(owner, repo, ref).statuses.orEmpty().map { GiteaCommitStatus.fromDto(it) }

    // ── Markdown ──────────────────────────────────────────────────────────

    /**
     * Renders a PR/issue/comment body to sanitized HTML via the server's markdown renderer.
     * Returns null on any failure — callers keep showing their escaped-plain-text fallback.
     */
    suspend fun renderMarkdown(markdown: String): String? =
        service<GiteaMarkdownService>().render(ctx.api, ctx.repo.repositoryPath.fullPath(), markdown)
}

/**
 * Pure merge of the raw timeline endpoint with the reviews and commits endpoints into a single
 * chronologically-ordered [GiteaTimelineItem] list. Kept top-level so it is unit-testable without
 * a live API.
 *
 * The timeline endpoint is the source of truth for ordering, events and conversation comments;
 * reviews are joined in by id (de-duplicated), and commits come only from the commits endpoint
 * (the timeline's own `commit_ref`/`pull_push` rows are skipped to avoid duplicates).
 */
fun mergeTimeline(
    timeline: List<TimelineComment>,
    reviewsById: Map<Long, GiteaReview>,
    threadsByReviewId: Map<Long, List<GiteaReviewThread>>,
    commits: List<GiteaCommit>,
): List<GiteaTimelineItem> {
    val items = mutableListOf<GiteaTimelineItem>()
    val seenReviews = mutableSetOf<Long>()

    fun reviewItem(reviewId: Long, fallbackActor: GiteaUser?, fallbackTs: java.util.Date, fallbackBody: String?) {
        if (!seenReviews.add(reviewId)) return
        val review = reviewsById[reviewId]
        items += GiteaTimelineItem.Review(
            id = reviewId,
            actor = review?.author ?: fallbackActor,
            timestamp = review?.submittedAt ?: fallbackTs,
            state = review?.state ?: GiteaReviewState.COMMENT,
            body = review?.body ?: fallbackBody,
            htmlUrl = review?.htmlUrl,
            threads = threadsByReviewId[reviewId].orEmpty(),
        )
    }

    for (tc in timeline) {
        val ts = tc.createdAt?.toDate() ?: continue
        val actor = tc.user?.let { GiteaUser.fromDto(it) }
        val reviewId = tc.reviewId?.takeIf { it != 0L }
        when (tc.type) {
            "comment" ->
                if (reviewId != null) {
                    reviewItem(reviewId, actor, ts, tc.body)
                } else if (!tc.body.isNullOrBlank()) {
                    items += GiteaTimelineItem.Comment(tc.id ?: 0L, actor, ts, tc.body, tc.htmlUrl)
                }
            "review" -> if (reviewId != null) reviewItem(reviewId, actor, ts, tc.body)
            // Inline review comments live inside their review; commits come from the commits
            // endpoint; skip the timeline's own duplicates.
            "code", "pull_push", "commit_ref" -> Unit
            else -> tc.toTimelineItemOrNull()?.let { items += it }
        }
    }

    for (commit in commits) {
        val ts = commit.createdAt ?: continue
        val sha = commit.sha.ifEmpty { null } ?: continue
        items += GiteaTimelineItem.Commit(
            id = sha.hashCode().toLong(),
            actor = commit.author,
            timestamp = ts,
            sha = sha,
            shortSha = sha.take(7),
            messageTitle = commit.messageTitle,
            htmlUrl = commit.htmlUrl,
        )
    }

    return items.sortedBy { it.timestamp }
}
