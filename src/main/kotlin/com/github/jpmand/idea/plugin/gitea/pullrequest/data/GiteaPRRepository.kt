package com.github.jpmand.idea.plugin.gitea.pullrequest.data

import com.github.jpmand.idea.plugin.gitea.api.*
import com.github.jpmand.idea.plugin.gitea.api.models.*
import com.github.jpmand.idea.plugin.gitea.api.rest.*
import com.github.jpmand.idea.plugin.gitea.api.rest.dto.*
import com.github.jpmand.idea.plugin.gitea.api.rest.pr.*
import com.github.jpmand.idea.plugin.gitea.pullrequest.diff.GiteaPRChangedFile
import com.github.jpmand.idea.plugin.gitea.pullrequest.diff.toChangedFile
import com.github.jpmand.idea.plugin.gitea.pullrequest.ui.timeline.GiteaPRTimelineItemViewModel
import com.intellij.collaboration.api.HttpStatusErrorException
import java.util.*

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

    /** The signed-in account this repository is scoped to — used for virtual-file identity so a
     * diff/timeline tab from a stale account context is never conflated with a fresh one. */
    val accountId: String get() = ctx.account.id

    /** The signed-in account's login — gates inline-comment edit/delete/reply controls to a
     * comment's own author (Gitea's API exposes no `viewerCanUpdate`-style flag). */
    val accountLogin: String get() = ctx.account.name

    /** The authenticated API client — used to build an avatar-icons loader for the diff-editor
     * review UI, the one place that still needs raw API access outside this repository. */
    val api: GiteaApi get() = ctx.api

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

    suspend fun resolveComment(commentId: Long) = ctx.api.repoResolvePullRequestReviewComment(owner, repo, commentId)

    suspend fun unresolveComment(commentId: Long) = ctx.api.repoUnresolvePullRequestReviewComment(owner, repo, commentId)

    suspend fun submitReview(prNumber: Int, body: CreatePullReviewOptions): GiteaReview =
        GiteaReview.fromDto(ctx.api.repoCreatePullRequestReview(owner, repo, prNumber, body))

    /** Submits (finishes) a review that was previously created with `event = PENDING`. */
    suspend fun submitPendingReview(prNumber: Int, reviewId: Long, body: SubmitPullReviewOptions): GiteaReview =
        GiteaReview.fromDto(ctx.api.repoSubmitPullRequestReview(owner, repo, prNumber, reviewId, body))

    /** Permanently deletes a pending (not yet submitted) review and its comments — used to cancel
     * a review-in-progress, whether started here or forgotten from another session. */
    suspend fun deletePendingReview(prNumber: Int, reviewId: Long) =
        ctx.api.repoDeletePullRequestReview(owner, repo, prNumber, reviewId)

    /** The signed-in account's own not-yet-submitted review for this PR, if any. */
    suspend fun findMyPendingReview(prNumber: Int): GiteaReview? =
        loadReviews(prNumber).firstOrNull { it.state == GiteaReviewState.PENDING && it.author?.login == ctx.account.name }

    /** Posts a new top-level (non-inline) timeline comment. */
    suspend fun createComment(prNumber: Int, body: String): GiteaPRTimelineItemViewModel.Comment {
        val comment = ctx.api.repoCreatePullRequestComment(owner, repo, prNumber, CreateIssueCommentOption(body))
        return GiteaPRTimelineItemViewModel.Comment(
            comment.id ?: 0L,
            comment.user?.let { GiteaUser.fromDto(it) },
            comment.createdAt?.toDate() ?: Date(),
            comment.body,
            comment.htmlUrl,
            comment.updatedAt?.toDate(),
        )
    }

    /** The signed-in account's own profile — used for the "leave a comment" avatar. */
    suspend fun currentUser(): GiteaUser = ctx.api.currentUser()

    /**
     * Edits an existing comment's body. Gitea uses the same endpoint for both top-level timeline
     * comments and inline review-thread comments — no distinction is needed here.
     */
    suspend fun editComment(commentId: Long, body: String) {
        ctx.api.repoEditPullRequestComment(owner, repo, commentId, EditIssueCommentOption(body))
    }

    /** Deletes a comment (top-level or inline review-thread). */
    suspend fun deleteComment(commentId: Long) {
        ctx.api.repoDeletePullRequestComment(owner, repo, commentId)
    }

    /** Replies to an existing (already-submitted) inline review comment — always immediate,
     * unrelated to review-batch submission. */
    suspend fun replyToComment(prNumber: Int, commentId: Long, body: String): GiteaReviewComment =
        GiteaReviewComment.fromDto(
            ctx.api.repoCreatePullReviewCommentReply(owner, repo, prNumber, commentId, CreatePullReviewCommentReplyOptions(body)),
        )

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
                    items += GiteaTimelineItem.Comment(tc.id ?: 0L, actor, ts, tc.body, tc.htmlUrl, tc.updatedAt?.toDate())
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
            rawAuthor = commit.authorName,
            timestamp = ts,
            sha = sha,
            shortSha = sha.take(7),
            messageTitle = commit.messageTitle,
            htmlUrl = commit.htmlUrl,
        )
    }

    return items.sortedBy { it.timestamp }
}
