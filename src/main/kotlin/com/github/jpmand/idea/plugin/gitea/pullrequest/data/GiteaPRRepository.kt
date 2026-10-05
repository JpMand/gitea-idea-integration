package com.github.jpmand.idea.plugin.gitea.pullrequest.data

import com.github.jpmand.idea.plugin.gitea.api.*
import com.github.jpmand.idea.plugin.gitea.api.models.*
import com.github.jpmand.idea.plugin.gitea.api.rest.*
import com.github.jpmand.idea.plugin.gitea.api.rest.dto.*
import com.github.jpmand.idea.plugin.gitea.api.rest.pr.*
import com.github.jpmand.idea.plugin.gitea.pullrequest.diff.GiteaPRChangedFile
import com.github.jpmand.idea.plugin.gitea.pullrequest.diff.parseDiffChangedFiles
import com.github.jpmand.idea.plugin.gitea.pullrequest.diff.toChangedFile
import com.github.jpmand.idea.plugin.gitea.pullrequest.ui.timeline.GiteaPRTimelineItemViewModel
import com.intellij.collaboration.api.HttpStatusErrorException
import com.intellij.openapi.diagnostic.logger
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.util.*

private val LOG = logger<GiteaPRRepository>()

/** How many requests of one batch (review comments, file contents) run at the same time. */
private const val PARALLEL_REQUESTS = 4

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

    private val shared: GiteaSharedLoads get() = ctx.sharedLoads

    /** See [GiteaPRDataContext.avatarImages]. */
    val avatarImages: GiteaSharedLoads get() = ctx.avatarImages

    /** Forgets shared results, so the next loads go to the server — for an explicit refresh. */
    fun dropSharedLoads() = shared.clear()

    /** A [giteaApiCall] that changes data on the server: drops shared results afterwards, even if
     * it failed part-way, so the reload that usually follows sees the server's state. */
    private suspend fun <T> changeCall(call: suspend () -> T): T =
        try {
            giteaApiCall(call)
        } finally {
            shared.clear()
        }

    private suspend fun <T> parallel(items: List<T>, limit: Semaphore, load: suspend (T) -> Unit) = coroutineScope {
        items.map { item -> async { limit.withPermit { load(item) } } }.awaitAll()
    }

    // ── Pull Requests ─────────────────────────────────────────────────────

    suspend fun loadPullRequests(
        state: String? = "open",
        sort: GiteaPullRequestSortEnum? = null,
        labels: List<Long>? = null,
        poster: String? = null,
        page: Int? = null,
        limit: Int? = null,
    ): List<GiteaPullRequest> = giteaApiCall {
        ctx.api.repoListPullRequests(owner, repo, null, state, sort, null, labels, poster, page, limit)
            .map { GiteaPullRequest.fromDto(it) }
    }

    /** Repository labels, for the PR-list "Label" filter. */
    suspend fun loadLabels(): List<GiteaLabel> = giteaApiCall {
        loadAllGiteaPages { page -> ctx.api.repoListLabels(owner, repo, page = page, limit = GITEA_PAGE_SIZE) }
            .map { GiteaLabel.fromDto(it) }
    }

    /**
     * Candidate PR authors for the "Author" filter — the repo's collaborators. Returns an empty
     * list (rather than throwing) only on 403, i.e. when the token lacks permission to enumerate
     * collaborators; any other failure gets the friendly [GiteaHttpError] treatment.
     */
    suspend fun loadPossibleAuthors(): List<GiteaUser> = shared.load("collaborators") { loadPossibleAuthorsNow() }

    private suspend fun loadPossibleAuthorsNow(): List<GiteaUser> = giteaApiCall {
        try {
            loadAllCollaborators()
        } catch (e: HttpStatusErrorException) {
            if (e.statusCode != 403) throw e
            LOG.debug("$owner/$repo: no permission to list collaborators (403)")
            emptyList()
        }
    }

    suspend fun loadPullRequest(number: Int): GiteaPullRequest = shared.load("pr" to number) {
        giteaApiCall { GiteaPullRequest.fromDto(ctx.api.repoGetPullRequest(owner, repo, number)) }
    }

    suspend fun editPullRequest(number: Int, body: EditPullRequestOption): GiteaPullRequest = changeCall {
        GiteaPullRequest.fromDto(ctx.api.repoEditPullRequest(owner, repo, number, body))
    }

    suspend fun mergePullRequest(number: Int, body: MergePullRequestOption) = changeCall {
        ctx.api.repoMergePullRequest(owner, repo, number, body)
    }

    // ── Reviewers ────────────────────────────────────────────────────────

    suspend fun requestReviewers(prNumber: Int, logins: List<String>) = changeCall {
        if (logins.isEmpty()) return@changeCall
        ctx.api.repoCreatePullReviewRequests(owner, repo, prNumber, PullReviewRequestOptions(reviewers = logins.toTypedArray()))
    }

    suspend fun removeReviewRequest(prNumber: Int, logins: List<String>) = changeCall {
        if (logins.isEmpty()) return@changeCall
        ctx.api.repoDeletePullReviewRequests(owner, repo, prNumber, PullReviewRequestOptions(reviewers = logins.toTypedArray()))
    }

    /**
     * Candidate reviewers for the Request Review picker: either the repo's collaborators, or every
     * user on the instance (via `/users/search`, which needs no admin rights), per [listAllUsers]
     * — see `GiteaSettings.allUsersArePotentialReviewers`. Falls back to the collaborators list whenever
     * the "all users" search is forbidden or comes back empty, same 403-tolerant treatment as
     * [loadPossibleAuthors].
     */
    suspend fun loadPossibleReviewers(listAllUsers: Boolean): List<GiteaUser> = giteaApiCall {
        if (listAllUsers) {
            val users = try {
                ctx.api.userSearch(limit = 100).data.orEmpty().map { GiteaUser.fromDto(it) }
            } catch (e: HttpStatusErrorException) {
                if (e.statusCode != 403) throw e
                LOG.debug("No permission to search users (403), falling back to collaborators")
                emptyList()
            }
            if (users.isNotEmpty()) return@giteaApiCall users
        }
        try {
            loadAllCollaborators()
        } catch (e: HttpStatusErrorException) {
            if (e.statusCode != 403) throw e
            LOG.debug("$owner/$repo: no permission to list collaborators (403)")
            emptyList()
        }
    }

    /** Every page — the server caps a page at 50, whatever `limit` asks for. */
    private suspend fun loadAllCollaborators(): List<GiteaUser> =
        loadAllGiteaPages { page -> ctx.api.repoListCollaborators(owner, repo, page = page, limit = GITEA_PAGE_SIZE) }
            .map { GiteaUser.fromDto(it) }

    // ── Reviews & Comments ────────────────────────────────────────────────

    suspend fun loadReviews(prNumber: Int): List<GiteaReview> = shared.load("reviews" to prNumber) {
        giteaApiCall {
            loadAllGiteaPages { page -> ctx.api.repoListPullRequestReviews(owner, repo, prNumber, page = page, limit = GITEA_PAGE_SIZE) }
                .map { GiteaReview.fromDto(it) }
        }
    }

    suspend fun loadReviewComments(prNumber: Int, reviewId: Long): List<GiteaReviewComment> =
        shared.load(Triple("review comments", prNumber, reviewId)) {
            giteaApiCall {
                ctx.api.repoGetPullRequestReviewComments(owner, repo, prNumber, reviewId)
                    .map { GiteaReviewComment.fromDto(it) }
            }
        }

    /** The comments of all [reviews] (or the PR's), a few reviews at a time, in review order.
     * Reviews without comments (most approvals) aren't asked. */
    suspend fun loadAllReviewComments(prNumber: Int, reviews: List<GiteaReview>? = null): List<GiteaReviewComment> {
        val withComments = (reviews ?: loadReviews(prNumber)).filter { it.commentsCount > 0 }
        val comments = arrayOfNulls<List<GiteaReviewComment>>(withComments.size)
        parallel(withComments.indices.toList(), Semaphore(PARALLEL_REQUESTS)) { i ->
            comments[i] = loadReviewComments(prNumber, withComments[i].id)
        }
        return comments.flatMap { it.orEmpty() }
    }

    /** Groups all review comments for a PR into synthetic [GiteaReviewThread]s, with
     * [GiteaReviewThread.isOutdated] computed against the file content at [headSha] and
     * [GiteaReviewThread.reviewCommitId] set. */
    suspend fun loadThreads(prNumber: Int, headSha: String): List<GiteaReviewThread> {
        val reviews = loadReviews(prNumber)
        val threads = loadAllReviewComments(prNumber, reviews).toThreads().withReviewCommits(reviews)
        return markOutdated(threads, reviews, headSha, mutableMapOf())
    }

    /**
     * Sets [GiteaReviewThread.isOutdated] on [threads]. Only threads whose anchor comment belongs to
     * a review Gitea reports as `stale` (made against an older head) can be outdated, so the head
     * file content is fetched just for those paths, once each via [headLinesCache].
     */
    private suspend fun markOutdated(
        threads: List<GiteaReviewThread>,
        reviews: List<GiteaReview>,
        headSha: String,
        headLinesCache: MutableMap<String, List<String>>,
    ): List<GiteaReviewThread> {
        val staleReviewIds = reviews.filter { it.stale }.mapTo(HashSet()) { it.id }
        val pathsToCheck = threads
            .filter { it.comments.firstOrNull()?.reviewId in staleReviewIds }
            .mapNotNull { it.path }
            .distinct()
            .filter { it !in headLinesCache }
        val loaded = java.util.concurrent.ConcurrentHashMap<String, List<String>>()
        parallel(pathsToCheck, Semaphore(PARALLEL_REQUESTS)) { path -> loaded[path] = loadFileContent(path, headSha).lines() }
        headLinesCache.putAll(loaded)
        return threads.map { thread ->
            val anchor = thread.comments.firstOrNull()
            val path = thread.path
            if (anchor == null || path == null || anchor.reviewId !in staleReviewIds) return@map thread
            val headLines = headLinesCache[path] ?: loadFileContent(path, headSha).lines().also { headLinesCache[path] = it }
            thread.copy(isOutdated = isAnchorOutdated(anchor, headLines))
        }
    }

    /**
     * The PR's full activity timeline (Conversation): comments, commits, submitted reviews (with
     * their inline threads), and metadata events, in chronological order.
     */
    suspend fun loadTimeline(prNumber: Int, headSha: String): List<GiteaTimelineItem> = coroutineScope {
        // The timeline, the threads and the commits don't depend on each other.
        val timeline = async {
            giteaApiCall { loadAllGiteaPages { page -> ctx.api.issueListTimeline(owner, repo, prNumber, page = page, limit = GITEA_PAGE_SIZE) } }
        }
        val commits = async { loadCommits(prNumber) }
        val reviews = loadReviews(prNumber)
        val headLinesCache = mutableMapOf<String, List<String>>()
        val threadsByReviewId = loadAllReviewComments(prNumber, reviews)
            .groupBy { it.reviewId ?: 0L }
            .mapValues { (_, comments) -> markOutdated(comments.toThreads().withReviewCommits(reviews), reviews, headSha, headLinesCache) }
        mergeTimeline(timeline.await(), reviews.associateBy { it.id }, threadsByReviewId, commits.await())
    }

    suspend fun resolveComment(commentId: Long) = changeCall {
        ctx.api.repoResolvePullRequestReviewComment(owner, repo, commentId)
    }

    suspend fun unresolveComment(commentId: Long) = changeCall {
        ctx.api.repoUnresolvePullRequestReviewComment(owner, repo, commentId)
    }

    suspend fun submitReview(prNumber: Int, body: CreatePullReviewOptions): GiteaReview = changeCall {
        GiteaReview.fromDto(ctx.api.repoCreatePullRequestReview(owner, repo, prNumber, body))
    }

    /** Submits (finishes) a review that was previously created with `event = PENDING`. */
    suspend fun submitPendingReview(prNumber: Int, reviewId: Long, body: SubmitPullReviewOptions): GiteaReview = changeCall {
        GiteaReview.fromDto(ctx.api.repoSubmitPullRequestReview(owner, repo, prNumber, reviewId, body))
    }

    /** Permanently deletes a pending (not yet submitted) review and its comments — used to cancel
     * a review-in-progress, whether started here or forgotten from another session. */
    suspend fun deletePendingReview(prNumber: Int, reviewId: Long) = changeCall {
        ctx.api.repoDeletePullRequestReview(owner, repo, prNumber, reviewId)
    }

    /** The signed-in account's own not-yet-submitted review for this PR, if any. */
    suspend fun findMyPendingReview(prNumber: Int): GiteaReview? = findMyPendingReviews(prNumber).firstOrNull()

    /** All of the signed-in user's pending reviews on the PR (normally at most one). */
    suspend fun findMyPendingReviews(prNumber: Int): List<GiteaReview> =
        loadReviews(prNumber).filter { it.state == GiteaReviewState.PENDING && it.author?.login == ctx.account.name }

    /** Posts a new top-level (non-inline) timeline comment. */
    suspend fun createComment(prNumber: Int, body: String): GiteaPRTimelineItemViewModel.Comment = changeCall {
        val comment = ctx.api.repoCreatePullRequestComment(owner, repo, prNumber, CreateIssueCommentOption(body))
        GiteaPRTimelineItemViewModel.Comment(
            comment.id ?: 0L,
            comment.user?.let { GiteaUser.fromDto(it) },
            comment.createdAt?.toDate() ?: Date(),
            comment.body,
            comment.htmlUrl,
            comment.updatedAt?.toDate(),
        )
    }

    /** The signed-in account's own profile — used for the "leave a comment" avatar. */
    suspend fun currentUser(): GiteaUser = shared.load("current user") { giteaApiCall { ctx.api.currentUser() } }

    /**
     * Edits an existing comment's body. Gitea uses the same endpoint for both top-level timeline
     * comments and inline review-thread comments — no distinction is needed here.
     */
    suspend fun editComment(commentId: Long, body: String) = changeCall {
        ctx.api.repoEditPullRequestComment(owner, repo, commentId, EditIssueCommentOption(body))
    }

    /** Deletes a comment (top-level or inline review-thread). */
    suspend fun deleteComment(commentId: Long) = changeCall {
        ctx.api.repoDeletePullRequestComment(owner, repo, commentId)
    }

    /** Replies to an existing (already-submitted) inline review comment — always immediate,
     * unrelated to review-batch submission. */
    suspend fun replyToComment(prNumber: Int, commentId: Long, body: String): GiteaReviewComment = changeCall {
        GiteaReviewComment.fromDto(
            ctx.api.repoCreatePullReviewCommentReply(owner, repo, prNumber, commentId, CreatePullReviewCommentReplyOptions(body)),
        )
    }

    // ── Files & Commits ───────────────────────────────────────────────────

    /** Returns domain models for files changed in the given PR (base..head). */
    suspend fun loadChangedFiles(prNumber: Int): List<GiteaPRChangedFile> = shared.load("files" to prNumber) {
        giteaApiCall {
            loadAllGiteaPages { page -> ctx.api.repoListPullRequestFiles(owner, repo, prNumber, page = page, limit = GITEA_PAGE_SIZE) }
                .map { it.toChangedFile() }
        }
    }

    /** Returns domain models for files changed by a single commit. */
    suspend fun loadCommitChangedFiles(sha: String): List<GiteaPRChangedFile> = shared.load("commit files" to sha) {
        giteaApiCall {
            ctx.api.repoGetSingleCommit(owner, repo, sha).files.orEmpty().map { it.toChangedFile() }
        }
    }

    /** Returns domain models for files changed from [baseSha] to [headSha], e.g. the PR as it was
     * at an older head — read from the raw diff, the one comparison Gitea lists files in. */
    suspend fun loadComparedFiles(baseSha: String, headSha: String): List<GiteaPRChangedFile> =
        shared.load(Triple("compared files", baseSha, headSha)) {
            giteaApiCall { parseDiffChangedFiles(ctx.api.repoCompareDiff(owner, repo, baseSha, headSha)) }
        }

    /**
     * Fetches the raw text content of a file at a specific ref (branch name, tag, or SHA).
     * Returns an empty string only when the file genuinely does not exist at that ref (404 — the
     * normal case for the base side of an added file or the head side of a deleted one). Any other
     * failure gets the friendly [GiteaHttpError] treatment, so a transient error is not silently
     * rendered as a whole-file add/delete.
     */
    suspend fun loadFileContent(path: String, ref: String): String = shared.load(Triple("file", path, ref)) { loadFileContentNow(path, ref) }

    private suspend fun loadFileContentNow(path: String, ref: String): String = giteaApiCall {
        try {
            ctx.api.getFileContents(owner, repo, path, ref).decodeContent() ?: ""
        } catch (e: HttpStatusErrorException) {
            if (e.statusCode != 404) throw e
            LOG.debug("$path doesn't exist at $ref (404), using empty content")
            ""
        }
    }

    suspend fun loadCommits(prNumber: Int): List<GiteaCommit> = shared.load("commits" to prNumber) {
        giteaApiCall {
            loadAllGiteaPages { page -> ctx.api.repoListPullRequestCommits(owner, repo, prNumber, page = page, limit = GITEA_PAGE_SIZE) }
                .map { GiteaCommit.fromDto(it) }
        }
    }

    // ── CI Status ─────────────────────────────────────────────────────────

    suspend fun loadCombinedStatus(ref: String): List<GiteaCommitStatus> = giteaApiCall {
        ctx.api.repoCombinedStatus(owner, repo, ref).statuses.orEmpty().map { GiteaCommitStatus.fromDto(it) }
    }
}

/**
 * Pure merge of the raw timeline endpoint with the reviews and commits endpoints into a single
 * chronologically-ordered [GiteaTimelineItem] list. Kept top-level so it is unit-testable without
 * a live API.
 *
 * The timeline endpoint is the source of truth for ordering, events and conversation comments;
 * reviews are joined in by id (de-duplicated), and pushed commits come only from the commits
 * endpoint (the timeline's own `pull_push` rows are skipped to avoid duplicates) — `commit_ref`
 * is kept, since it's a reference to this PR from an unrelated commit, not a duplicate.
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
            // Inline review comments live inside their review; commits themselves come from the
            // commits endpoint (skip that duplicate) — but "commit_ref" is a *reference* to this
            // PR from an unrelated commit elsewhere, not a duplicate of a pushed commit, so it
            // falls through to toTimelineItemOrNull()'s own REFERENCED_FROM_COMMIT mapping.
            "code", "pull_push" -> Unit
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
