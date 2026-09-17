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
    /** The PR's current head SHA — a thread's anchor comment carrying a different `commitId`
     * means the diff it was anchored to is no longer the latest one, i.e. it's "outdated". */
    val headSha: String,
    private val repository: GiteaPRRepository,
) : CodeReviewInEditorViewModel {

    private val settings: GiteaPullRequestsSettings get() = project.service()

    /** The signed-in account's login — gates inline-comment edit/delete/reply controls to a
     * comment's own author, same as the Timeline's `currentUserLogin`. */
    val currentUserLogin: String get() = repository.accountLogin

    companion object {
        val CONTEXT_KEY: Key<GiteaPRDiscussionsViewModels> = Key.create("gitea.pr.discussions.vm")
    }

    private val cs = CoroutineScope(parentCs.coroutineContext + SupervisorJob(parentCs.coroutineContext[Job]))

    /** Avatar icons for comment/reply authors in the diff-editor review UI. */
    val avatars: IconsProvider<GiteaUser> =
        CachingIconsProvider(AsyncImageIconsProvider(cs, GiteaImageLoader(repository.api)))

    /** The signed-in account's own profile, loaded once — used for the reply composer's avatar. */
    private val _currentUser = MutableStateFlow<GiteaUser?>(null)
    val currentUser: StateFlow<GiteaUser?> = _currentUser.asStateFlow()

    /** Repo collaborators, loaded once for `@`-mention completion in reply composers — see
     * [com.github.jpmand.idea.plugin.gitea.pullrequest.ui.comment.mention.GiteaMentionCompletionContributor]. */
    private val _mentionCandidates = MutableStateFlow<List<GiteaUser>>(emptyList())
    val mentionCandidates: StateFlow<List<GiteaUser>> = _mentionCandidates.asStateFlow()

    /** The signed-in account's own not-yet-submitted review for this PR, if any — surfaced as a
     * "finish your review" prompt instead of the "start a review" composer. */
    private val _pendingReview = MutableStateFlow<GiteaReview?>(null)
    val pendingReview: StateFlow<GiteaReview?> = _pendingReview.asStateFlow()

    private val _isSubmittingReview = MutableStateFlow(false)
    val isSubmittingReview: StateFlow<Boolean> = _isSubmittingReview.asStateFlow()

    // ── Threads ───────────────────────────────────────────────────────────

    private val _reloadTrigger = MutableStateFlow(0)

    private val _threads = MutableStateFlow<ComputedResult<List<GiteaPRThreadViewModel>>?>(null)
    val threads: StateFlow<ComputedResult<List<GiteaPRThreadViewModel>>?> = _threads.asStateFlow()

    init {
        cs.launch(Dispatchers.IO) {
            _reloadTrigger.collectLatest {
                _threads.value = ComputedResult.loading()
                try {
                    val threadList = repository.loadThreads(prNumber)
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
            try {
                _mentionCandidates.value = repository.loadPossibleAuthors()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Best-effort — a failed lookup just means no mention completion, not an error banner.
            }
        }
        reloadPendingReview()
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

    private val _draftComments = MutableStateFlow(settings.draftComments(prNumber))
    val draftComments: StateFlow<List<GiteaPRDraftComment>> = _draftComments.asStateFlow()

    private var nextDraftId = (_draftComments.value.maxOfOrNull { it.localId } ?: -1L) + 1L

    private fun updateDrafts(transform: (List<GiteaPRDraftComment>) -> List<GiteaPRDraftComment>) {
        val next = transform(_draftComments.value)
        _draftComments.value = next
        settings.setDraftComments(prNumber, next)
    }

    /** Creates a new draft comment and returns it (its [GiteaPRDraftComment.localId] is assigned
     * here). Purely local — no network call. */
    fun addDraft(path: String, newLine: Int?, oldLine: Int?, body: String): GiteaPRDraftComment {
        val draft = GiteaPRDraftComment(nextDraftId++, path, newLine, oldLine, body)
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

    /**
     * Submits the current draft batch as a brand-new review with the given verdict — or, when
     * [event] is `PENDING`, creates a draft review that only becomes visible to others once
     * [submitPendingReview] finishes it. Clears local drafts and reloads on success.
     */
    fun submitReview(event: CreatePullReviewOptions.Event, body: String) {
        val comments = _draftComments.value.map {
            CreatePullReviewComment(body = it.body, path = it.path, newPosition = it.newLine?.toLong(), oldPosition = it.oldLine?.toLong())
        }
        cs.launch(Dispatchers.IO) {
            _isSubmittingReview.value = true
            try {
                repository.submitReview(
                    prNumber,
                    CreatePullReviewOptions(body = body.ifBlank { null }, comments = comments.toTypedArray(), commitId = headSha, event = event),
                )
                updateDrafts { emptyList() }
                reload()
                reloadPendingReview()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                notifyError("pull.request.action.submit.review.error")
            } finally {
                withContext(NonCancellable) { _isSubmittingReview.value = false }
            }
        }
    }

    /** Finishes (submits) the currently pending review with the given verdict — body/event only,
     * no new comments (Gitea's API has no way to add any to an already-created review). */
    fun submitPendingReview(event: SubmitPullReviewOptions.Event, body: String) {
        val reviewId = pendingReview.value?.id ?: return
        cs.launch(Dispatchers.IO) {
            _isSubmittingReview.value = true
            try {
                repository.submitPendingReview(prNumber, reviewId, SubmitPullReviewOptions(body = body.ifBlank { null }, event = event))
                reload()
                reloadPendingReview()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                notifyError("pull.request.action.submit.review.error")
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
                reload()
                reloadPendingReview()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                notifyError("pull.request.action.cancel.review.error")
            } finally {
                withContext(NonCancellable) { _isSubmittingReview.value = false }
            }
        }
    }

    private suspend fun notifyError(bundleKey: String) {
        withContext(Dispatchers.Main) {
            NotificationGroupManager.getInstance()
                .getNotificationGroup("Gitea")
                .createNotification(GiteaBundle.message(bundleKey), NotificationType.ERROR)
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
        reload()
    }

    /**
     * Unresolves the anchor comment of the thread identified by [threadId] and reloads.
     */
    suspend fun unresolveThread(threadId: Long) {
        repository.unresolveComment(threadId)
        reload()
    }

    /** Replies to the given review comment — callers pass the *last* comment in the thread they
     * mean to continue (see [GiteaPRThreadViewModel.lastCommentId]), not the anchor, so Gitea's
     * reply endpoint threads the conversation correctly. */
    suspend fun replyToThread(commentId: Long, body: String) {
        repository.replyToComment(prNumber, commentId, body)
        reload()
    }

    /** Edits an inline review comment's body (own comments only — gated by [currentUserLogin] at
     * the call site, same as the Timeline) and reloads. */
    suspend fun editComment(commentId: Long, body: String) {
        repository.editComment(commentId, body)
        reload()
    }

    /** Deletes an inline review comment (own comments only) and reloads. */
    suspend fun deleteComment(commentId: Long) {
        repository.deleteComment(commentId)
        reload()
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
