package com.github.jpmand.idea.plugin.gitea.pullrequest.review

import com.github.jpmand.idea.plugin.gitea.api.models.GiteaPRDraftComment
import com.github.jpmand.idea.plugin.gitea.api.models.GiteaReview
import com.github.jpmand.idea.plugin.gitea.api.models.GiteaReviewThread
import com.github.jpmand.idea.plugin.gitea.api.models.GiteaUser
import com.github.jpmand.idea.plugin.gitea.api.rest.dto.CreatePullReviewComment
import com.github.jpmand.idea.plugin.gitea.api.rest.dto.CreatePullReviewOptions
import com.github.jpmand.idea.plugin.gitea.api.rest.dto.SubmitPullReviewOptions
import com.github.jpmand.idea.plugin.gitea.data.GiteaImageLoader
import com.github.jpmand.idea.plugin.gitea.pullrequest.GiteaPullRequestsSettings
import com.github.jpmand.idea.plugin.gitea.pullrequest.data.GiteaPRRepository
import com.github.jpmand.idea.plugin.gitea.util.GiteaBundle
import com.github.jpmand.idea.plugin.gitea.util.GiteaUtil
import com.intellij.collaboration.async.mapState
import com.intellij.collaboration.ui.codereview.diff.DiscussionsViewOption
import com.intellij.collaboration.ui.codereview.editor.CodeReviewInEditorViewModel
import com.intellij.collaboration.ui.icon.AsyncImageIconsProvider
import com.intellij.collaboration.ui.icon.CachingIconsProvider
import com.intellij.collaboration.ui.icon.IconsProvider
import com.intellij.collaboration.util.ComputedResult
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Central ViewModel for the review discussion layer of a single PR.
 *
 * Responsibilities:
 * - Loads and groups review comments into synthetic [GiteaReviewThread]s
 * - Manages per-PR-persisted draft comments accumulated before review submission
 * - Provides resolve/unresolve operations (API call → automatic reload)
 *
 * Scoped to the PR panel lifetime (same scope as the diff VM).
 */
@Suppress("UnstableApiUsage")
class GiteaPRDiscussionsViewModels(
    private val project: Project,
    parentCs: CoroutineScope,
    private val prNumber: Int,
    headSha: String,
    private val repository: GiteaPRRepository,
    /** Merged into [mentionCandidates] alongside repo collaborators — repo-wide collaborator
     * status isn't the only reason someone is mentionable on *this* PR specifically (e.g. a
     * requested reviewer, or the PR's own author, who may not be an explicit collaborator). */
    private val additionalMentionCandidates: List<GiteaUser> = emptyList(),
    /** The PR author's login — Gitea doesn't let authors approve or request changes on their own PR. */
    private val prAuthorLogin: String? = null,
) : CodeReviewInEditorViewModel {

    private val settings: GiteaPullRequestsSettings get() = project.service()

    /** The PR's current head SHA — new reviews are anchored to it, and threads are checked
     * against its file content to tell whether they're outdated. See [updateHeadSha]. */
    @Volatile
    var headSha: String = headSha
        private set

    /** Follows a new head (e.g. after a push and a refresh): reloads the threads when it changed. */
    fun updateHeadSha(sha: String) {
        if (sha == headSha) return
        headSha = sha
        reload()
    }

    /** The signed-in account's login — gates inline-comment edit/delete/reply controls to a
     * comment's own author, same as the Timeline's `currentUserLogin`. */
    val currentUserLogin: String get() = repository.accountLogin

    /** Whether the signed-in account opened this PR (it can then only leave comment reviews). */
    val viewerIsAuthor: Boolean get() = prAuthorLogin != null && prAuthorLogin.equals(currentUserLogin, ignoreCase = true)

    companion object {
        val CONTEXT_KEY: Key<GiteaPRDiscussionsViewModels> = Key.create("gitea.pr.discussions.vm")
    }

    private val cs = CoroutineScope(parentCs.coroutineContext + SupervisorJob(parentCs.coroutineContext[Job]))

    /** Lives as long as this PR's review UI — for UI work tied to it, e.g. the submit-review popup. */
    val scope: CoroutineScope get() = cs

    /** Avatar icons for comment/reply authors in the diff-editor review UI. */
    val avatars: IconsProvider<GiteaUser> =
        CachingIconsProvider(AsyncImageIconsProvider(cs, GiteaImageLoader(repository.api)))

    /** The signed-in account's own profile, loaded once — used for the reply composer's avatar. */
    private val _currentUser = MutableStateFlow<GiteaUser?>(null)
    val currentUser: StateFlow<GiteaUser?> = _currentUser.asStateFlow()

    /** Repo collaborators plus [additionalMentionCandidates], loaded once for `@`-mention
     * completion in reply composers — see
     * [com.github.jpmand.idea.plugin.gitea.pullrequest.ui.comment.mention.GiteaMentionCompletionContributor]. */
    private val _mentionCandidates = MutableStateFlow<List<GiteaUser>>(emptyList())
    val mentionCandidates: StateFlow<List<GiteaUser>> = _mentionCandidates.asStateFlow()

    /** The signed-in account's own not-yet-submitted review for this PR, if any — surfaced as a
     * "finish your review" prompt instead of the "start a review" composer. */
    private val _pendingReview = MutableStateFlow<GiteaReview?>(null)
    val pendingReview: StateFlow<GiteaReview?> = _pendingReview.asStateFlow()

    private val _isSubmittingReview = MutableStateFlow(false)
    val isSubmittingReview: StateFlow<Boolean> = _isSubmittingReview.asStateFlow()

    /** The body typed in the submit-review popup. Kept here, not in the popup, so closing the popup
     * (e.g. when it loses focus) and opening it again doesn't lose it; cleared once the review is
     * submitted or discarded. */
    val submitReviewText: MutableStateFlow<String> = MutableStateFlow("")

    // ── Threads ───────────────────────────────────────────────────────────

    private val _reloadTrigger = MutableStateFlow(0)

    private val _threads = MutableStateFlow<ComputedResult<List<GiteaPRThreadViewModel>>?>(null)
    val threads: StateFlow<ComputedResult<List<GiteaPRThreadViewModel>>?> = _threads.asStateFlow()

    init {
        cs.launch(Dispatchers.IO) {
            _reloadTrigger.collectLatest {
                _threads.value = ComputedResult.loading()
                try {
                    val threadList = repository.loadThreads(prNumber, headSha)
                    val threadVms = threadList.map { GiteaPRThreadViewModel(it, this@GiteaPRDiscussionsViewModels) }
                    _threads.value = ComputedResult.success(threadVms)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    _threads.value = ComputedResult.failure(e)
                }
            }
        }
        cs.launch(Dispatchers.IO) {
            try {
                _currentUser.value = repository.currentUser()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Best-effort — a failed lookup just means the reply composer stays hidden.
            }
        }
        cs.launch(Dispatchers.IO) {
            val collaborators = try {
                repository.loadPossibleAuthors()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Best-effort — a failed lookup still leaves additionalMentionCandidates usable.
                emptyList()
            }
            _mentionCandidates.value = (collaborators + additionalMentionCandidates).distinctBy { it.login }
        }
        reloadPendingReview()
        cs.launch {
            project.service<GiteaPRReviewChanges>().changes.collect { change ->
                if (change.prNumber == prNumber && change.source !== this@GiteaPRDiscussionsViewModels) {
                    reload()
                    reloadPendingReview()
                }
            }
        }
    }

    private fun reloadPendingReview() {
        cs.launch(Dispatchers.IO) {
            try {
                _pendingReview.value = repository.findMyPendingReview(prNumber)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Best-effort — a failed lookup just means no "finish your review" prompt shows.
            }
        }
    }

    /** Re-fetches all review comments from the API and rebuilds the thread list. */
    fun reload() {
        _reloadTrigger.value++
    }

    /** Reloads after a change made here, and tells this PR's other review surfaces to reload too. */
    private fun reloadAfterChange() {
        reload()
        reloadPendingReview()
        project.service<GiteaPRReviewChanges>().notifyChanged(prNumber, this)
    }

    // ── CodeReviewInEditorViewModel ───────────────────────────────────────

    private val _discussionsViewOption = MutableStateFlow(settings.diffReviewViewOption)
    override val discussionsViewOption: StateFlow<DiscussionsViewOption> = _discussionsViewOption.asStateFlow()
    override fun setDiscussionsViewOption(viewOption: DiscussionsViewOption) {
        _discussionsViewOption.value = viewOption
        settings.diffReviewViewOption = viewOption
    }

    /** Always false — Gitea plugin does not track local-branch sync state. */
    override val updateRequired: StateFlow<Boolean> = MutableStateFlow(false)

    /** No-op — `updateRequired` is always false so this button is never enabled. */
    override fun updateBranch() = Unit

    // ── Draft comments (not yet submitted) ──────────────────────────────────
    // Gitea has no endpoint to add a comment to an already-created review (pending or otherwise),
    // so the whole batch is built up here and submitted as one review — see
    // GiteaPRRepository.submitReview. Persisted per-PR via GiteaPullRequestsSettings so drafts
    // survive closing and reopening the diff/PR, not just in-memory for the panel's lifetime.

    // Read straight from the settings, so the Details tab, its diff and the regular editor (each
    // with its own view model) all see the same drafts.
    private val _draftComments: StateFlow<List<GiteaPRDraftComment>> = settings.draftCommentsState(prNumber)
    val draftComments: StateFlow<List<GiteaPRDraftComment>> = _draftComments

    private fun updateDrafts(transform: (List<GiteaPRDraftComment>) -> List<GiteaPRDraftComment>) {
        settings.setDraftComments(prNumber, transform(_draftComments.value))
    }

    /** Creates a new draft comment and returns it (its [GiteaPRDraftComment.localId] is assigned
     * here). Purely local — no network call. */
    fun addDraft(path: String, newLine: Int?, oldLine: Int?, body: String): GiteaPRDraftComment {
        val nextId = (_draftComments.value.maxOfOrNull { it.localId } ?: -1L) + 1L
        val draft = GiteaPRDraftComment(nextId, path, newLine, oldLine, body)
        updateDrafts { it + draft }
        return draft
    }

    /** Replaces a draft's body in place (identified by [GiteaPRDraftComment.localId]). */
    fun updateDraft(localId: Long, body: String) {
        updateDrafts { drafts -> drafts.map { if (it.localId == localId) it.copy(body = body) else it } }
    }

    /** Removes a not-yet-submitted draft comment. Purely local — no network call. */
    fun removeDraft(localId: Long) {
        updateDrafts { drafts -> drafts.filterNot { it.localId == localId } }
    }

    /** Returns all current drafts for a specific file path. */
    fun draftsForPath(path: String): List<GiteaPRDraftComment> = _draftComments.value.filter { it.path == path }

    /** How many local draft comments are waiting to be submitted. */
    val draftCommentsCount: StateFlow<Int> = _draftComments.mapState { it.size }

    /**
     * Submits the review-in-progress with [verdict]: finishes the signed-in user's pending review
     * when there is one, otherwise submits the local drafts as a new review. See [submitReview]
     * and [submitPendingReview] for [onSuccess]/[onError].
     */
    fun submit(verdict: GiteaReviewVerdict, body: String, onSuccess: () -> Unit = {}, onError: ((Throwable) -> Unit)? = null) {
        val pendingEvent = verdict.submitEvent
        if (pendingReview.value != null && pendingEvent != null) {
            submitPendingReview(pendingEvent, body, onSuccess, onError)
        } else {
            submitReview(verdict.createEvent, body, onSuccess, onError)
        }
    }

    /**
     * Submits the current draft batch as a brand-new review with the given verdict — or, when
     * [event] is `PENDING`, creates a draft review that only becomes visible to others once
     * [submitPendingReview] finishes it. Clears local drafts and reloads on success, then runs
     * [onSuccess] on the UI thread.
     *
     * A submission Gitea would reject (see [reviewSubmitProblem]) isn't sent. Failures go to
     * [onError] on the UI thread when given, otherwise to an error notification carrying the
     * server's message.
     */
    fun submitReview(
        event: CreatePullReviewOptions.Event,
        body: String,
        onSuccess: () -> Unit = {},
        onError: ((Throwable) -> Unit)? = null,
    ) {
        val drafts = _draftComments.value
        val verdict = GiteaReviewVerdict.entries.first { it.createEvent == event }
        launchSubmission(reviewSubmitProblem(verdict, body, drafts.size), onSuccess, onError) {
            // Asked here rather than read from pendingReview, which loads in the background and may
            // not have arrived yet: a pending review that already existed must never be discarded.
            val existingPending = try {
                PendingReviewBaseline.Known(repository.findMyPendingReviews(prNumber).mapTo(HashSet()) { it.id })
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                PendingReviewBaseline.Unknown
            }
            val comments = drafts.map {
                CreatePullReviewComment(body = it.body, path = it.path, newPosition = it.newLine?.toLong(), oldPosition = it.oldLine?.toLong())
            }
            try {
                repository.submitReview(
                    prNumber,
                    CreatePullReviewOptions(body = body.ifBlank { null }, comments = comments.toTypedArray(), commitId = headSha, event = event),
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (existingPending is PendingReviewBaseline.Known) discardOrphanedPendingReviews(existingPending.reviewIds)
                throw e
            }
            updateDrafts { emptyList() }
        }
    }

    /**
     * Gitea creates a review before adding its comments, and doesn't roll it back when adding one
     * fails (e.g. a 500 for a line the file doesn't have): the failed request leaves an empty
     * pending review behind, which then shows as "Finish review" with nothing in it. Deletes such
     * reviews — only empty ones, and never one of [existingReviewIds], the pending reviews that were
     * already there before the request — and reloads the pending-review state either way.
     * Best-effort: the original failure is what gets reported.
     */
    private suspend fun discardOrphanedPendingReviews(existingReviewIds: Set<Long>) {
        try {
            orphanedPendingReviews(repository.findMyPendingReviews(prNumber), existingReviewIds)
                .forEach { repository.deletePendingReview(prNumber, it.id) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            GiteaUtil.LOG.warn("Couldn't discard the empty pending review left by a failed submission", e)
        }
        reloadPendingReview()
    }

    /** Finishes (submits) the currently pending review with the given verdict — body/event only,
     * no new comments (Gitea's API has no way to add any to an already-created review). Same
     * [onSuccess]/[onError] contract as [submitReview]. */
    fun submitPendingReview(
        event: SubmitPullReviewOptions.Event,
        body: String,
        onSuccess: () -> Unit = {},
        onError: ((Throwable) -> Unit)? = null,
    ) {
        val pending = pendingReview.value ?: return
        val verdict = GiteaReviewVerdict.entries.first { it.submitEvent == event }
        launchSubmission(reviewSubmitProblem(verdict, body, pending.commentsCount), onSuccess, onError) {
            repository.submitPendingReview(prNumber, pending.id, SubmitPullReviewOptions(body = body.ifBlank { null }, event = event))
        }
    }

    private fun launchSubmission(
        problemKey: String?,
        onSuccess: () -> Unit,
        onError: ((Throwable) -> Unit)?,
        send: suspend () -> Unit,
    ) {
        cs.launch(Dispatchers.IO) {
            _isSubmittingReview.value = true
            try {
                if (problemKey != null) throw IllegalArgumentException(GiteaBundle.message(problemKey))
                send()
                submitReviewText.value = ""
                reloadAfterChange()
                withContext(Dispatchers.Main) { onSuccess() }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (onError != null) withContext(Dispatchers.Main) { onError(e) }
                else notifyError("pull.request.action.submit.review.error", e)
            } finally {
                withContext(NonCancellable) { _isSubmittingReview.value = false }
            }
        }
    }

    /**
     * Discards the review-in-progress: local drafts are always cleared, and if a pending review
     * already exists on the server (started here, or forgotten from another session — see the
     * diff-review milestone's TODO note on this), it and its already-posted comments are permanently
     * deleted, not just hidden.
     */
    fun cancelReview() {
        val pending = pendingReview.value
        cs.launch(Dispatchers.IO) {
            _isSubmittingReview.value = true
            try {
                if (pending != null) repository.deletePendingReview(prNumber, pending.id)
                updateDrafts { emptyList() }
                submitReviewText.value = ""
                reloadAfterChange()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                notifyError("pull.request.action.cancel.review.error", e)
            } finally {
                withContext(NonCancellable) { _isSubmittingReview.value = false }
            }
        }
    }

    /** An error notification titled by [bundleKey], with [cause]'s message (Gitea's own, for
     * API failures — see [com.github.jpmand.idea.plugin.gitea.api.GiteaHttpError]) as its text. */
    private suspend fun notifyError(bundleKey: String, cause: Throwable? = null) {
        withContext(Dispatchers.Main) {
            NotificationGroupManager.getInstance()
                .getNotificationGroup("Gitea")
                .createNotification(GiteaBundle.message(bundleKey), cause?.localizedMessage.orEmpty(), NotificationType.ERROR)
                .notify(project)
        }
    }

    // ── Resolve / unresolve / reply ─────────────────────────────────────────

    /**
     * Resolves the anchor comment of the thread identified by [threadId]
     * (= anchor comment ID = thread's synthetic ID) and reloads the thread list.
     */
    suspend fun resolveThread(threadId: Long) {
        repository.resolveComment(threadId)
        reloadAfterChange()
    }

    /**
     * Unresolves the anchor comment of the thread identified by [threadId] and reloads.
     */
    suspend fun unresolveThread(threadId: Long) {
        repository.unresolveComment(threadId)
        reloadAfterChange()
    }

    /** Replies to the given review comment — callers pass the *last* comment in the thread they
     * mean to continue (see [GiteaPRThreadViewModel.lastCommentId]), not the anchor, so Gitea's
     * reply endpoint threads the conversation correctly. */
    suspend fun replyToThread(commentId: Long, body: String) {
        repository.replyToComment(prNumber, commentId, body)
        reloadAfterChange()
    }

    /** Edits an inline review comment's body (own comments only — gated by [currentUserLogin] at
     * the call site, same as the Timeline) and reloads. */
    suspend fun editComment(commentId: Long, body: String) {
        repository.editComment(commentId, body)
        reloadAfterChange()
    }

    /** Deletes an inline review comment (own comments only) and reloads. */
    suspend fun deleteComment(commentId: Long) {
        repository.deleteComment(commentId)
        reloadAfterChange()
    }

    // ── Lookup helpers ────────────────────────────────────────────────────

    /**
     * Returns all loaded threads for a specific file path, sorted by line number.
     * Returns empty list if threads are not yet loaded.
     */
    fun threadsForPath(path: String): List<GiteaPRThreadViewModel> {
        val loaded = _threads.value?.result?.getOrNull() ?: return emptyList()
        return loaded
            .filter { it.path == path }
            .sortedWith(compareBy(nullsLast()) { it.newLine ?: it.oldLine })
    }
}

/** The pending reviews a failed submission left behind: empty ones that weren't among
 * [existingReviewIds] before it. */
internal fun orphanedPendingReviews(pendingReviews: List<GiteaReview>, existingReviewIds: Set<Long>): List<GiteaReview> =
    pendingReviews.filter { it.id !in existingReviewIds && it.commentsCount == 0 && it.body.isNullOrBlank() }

/** The signed-in user's pending reviews as they were right before a submission. */
private sealed interface PendingReviewBaseline {
    data class Known(val reviewIds: Set<Long>) : PendingReviewBaseline

    /** The lookup failed, so nothing left behind by a failed submission may be deleted. */
    data object Unknown : PendingReviewBaseline
}
