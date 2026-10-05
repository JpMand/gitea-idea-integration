package com.github.jpmand.idea.plugin.gitea.pullrequest.ui.details

import com.github.jpmand.idea.plugin.gitea.api.models.GiteaPullRequest
import com.github.jpmand.idea.plugin.gitea.api.models.GiteaUser
import com.github.jpmand.idea.plugin.gitea.api.rest.dto.EditPullRequestOption
import com.github.jpmand.idea.plugin.gitea.api.rest.dto.MergePullRequestOption
import com.github.jpmand.idea.plugin.gitea.pullrequest.data.GiteaPRRepository
import com.github.jpmand.idea.plugin.gitea.ui.GiteaSettings
import com.github.jpmand.idea.plugin.gitea.util.GiteaBundle
import com.github.jpmand.idea.plugin.gitea.util.GiteaUtil
import com.intellij.collaboration.ui.codereview.details.data.ReviewRequestState
import com.intellij.collaboration.ui.codereview.details.model.CodeReviewDetailsViewModel
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

private val LOG = logger<GiteaPRDetailsViewModel>()

/**
 * Details view model: title/description/status/branches/commits, plus close/reopen.
 * Merge and review submission are still stubbed in [GiteaPRDetailsPanel].
 */
@Suppress("UnstableApiUsage")
class GiteaPRDetailsViewModel(
    private val project: Project,
    private val cs: CoroutineScope,
    initialPr: GiteaPullRequest,
    private val repository: GiteaPRRepository,
) : CodeReviewDetailsViewModel {

    private val _pr = MutableStateFlow(initialPr)

    /** Live PR snapshot, refreshed after every close/reopen/merge/ready-for-review/request-review
     * call — [GiteaPRStatusViewModel] observes this to keep the conflict banner/merge gate and
     * reviewer badges in sync without a manual refresh. */
    val prFlow: StateFlow<GiteaPullRequest> = _pr.asStateFlow()

    val prNumber: Int = initialPr.number.toInt()

    /** Read without an account: the details show no write actions (merge, close, request review…). */
    val isAnonymous: Boolean get() = repository.isAnonymous

    override val number: String = "#${initialPr.number}"
    override val url: String = initialPr.htmlUrl

    @OptIn(ExperimentalCoroutinesApi::class)
    override val title: Flow<String> = _pr.map { it.title }.transformLatest { markdown ->
        emit(GiteaUtil.safeConvertMarkdownToHtml(markdown))
    }

    /**
     * Null when the initial PR has no body — skips the description pane entirely. Otherwise
     * emits an escaped-plain-text fallback immediately, then the server-rendered markdown HTML
     * once it's back (or never, if rendering fails — the fallback stays).
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    override val description: Flow<String>? =
        if (initialPr.body.isNullOrBlank()) null
        else _pr.map { it.body ?: "" }.transformLatest { markdown ->
            emit(GiteaUtil.safeConvertMarkdownToHtml(markdown))
        }

    override val reviewRequestState: Flow<ReviewRequestState> = _pr.map { pr ->
        when {
            pr.merged -> ReviewRequestState.MERGED
            pr.state == "closed" -> ReviewRequestState.CLOSED
            pr.draft -> ReviewRequestState.DRAFT
            else -> ReviewRequestState.OPENED
        }
    }

    val branchesVm = GiteaPRBranchesViewModel(project, cs, repository, _pr)
    val changesVm = GiteaPRChangesViewModel(cs, prNumber, repository)

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /** True while a close/reopen/merge call is in flight — disables their buttons to prevent a
     * double-submit (mirrors the bundled GitHub plugin's `reviewFlowVm.isBusy`-gated actions). */
    private val _isActionInProgress = MutableStateFlow(false)
    val isActionInProgress: StateFlow<Boolean> = _isActionInProgress.asStateFlow()

    private val _error = MutableStateFlow<Throwable?>(null)
    val error: StateFlow<Throwable?> = _error.asStateFlow()

    /** Re-fetches the PR and its commits. */
    fun refresh() {
        LOG.debug("PR #$prNumber: refreshing details")
        changesVm.reload()
        cs.launch(Dispatchers.IO) {
            _isLoading.value = true
            _error.value = null
            try {
                _pr.value = repository.loadPullRequest(prNumber)
                LOG.debug("PR #$prNumber: loaded (state ${_pr.value.state}, merged ${_pr.value.merged}, head ${_pr.value.head.sha})")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                LOG.warn("PR #$prNumber: couldn't load the pull request", e)
                _error.value = e
            } finally {
                withContext(NonCancellable) { _isLoading.value = false }
            }
        }
    }

    // ── Close / Reopen ───────────────────────────────────────────────────

    fun closePullRequest() = editState("closed", "pull.request.action.close.error")

    fun reopenPullRequest() = editState("open", "pull.request.action.reopen.error")

    private fun editState(state: String, errorKey: String) {
        LOG.info("PR #$prNumber: setting state to $state")
        cs.launch(Dispatchers.IO) {
            _isActionInProgress.value = true
            try {
                _pr.value = repository.editPullRequest(prNumber, EditPullRequestOption(state = state))
                LOG.info("PR #$prNumber: state is now ${_pr.value.state}")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                notifyError(errorKey, e)
            } finally {
                withContext(NonCancellable) { _isActionInProgress.value = false }
            }
        }
    }

    // ── Ready for review ─────────────────────────────────────────────────

    /**
     * Gitea has no dedicated "draft" flag to flip — the server derives [GiteaPullRequest.draft]
     * from a configurable title prefix (confirmed against `gitea-swagger-v2-spec.json`:
     * [EditPullRequestOption] has no `draft` field, and there is no separate toggle endpoint).
     * Gitea's own web UI does the same thing this does: edit the title to strip the prefix. Only
     * the two prefixes Gitea ships by default (`WIP:`, `[WIP]`, matched case-insensitively) are
     * recognized — a server configured with a custom prefix isn't knowable from the REST API, so
     * this silently does nothing on the title in that case (the PR stays a draft).
     */
    fun markReadyForReview() {
        val newTitle = DRAFT_PREFIX_RE.replaceFirst(_pr.value.title, "")
        LOG.info("PR #$prNumber: marking ready for review (draft prefix ${if (newTitle == _pr.value.title) "not found" else "removed"})")
        cs.launch(Dispatchers.IO) {
            _isActionInProgress.value = true
            try {
                _pr.value = repository.editPullRequest(prNumber, EditPullRequestOption(title = newTitle))
                LOG.info("PR #$prNumber: draft is now ${_pr.value.draft}")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                notifyError("pull.request.action.ready.for.review.error", e)
            } finally {
                withContext(NonCancellable) { _isActionInProgress.value = false }
            }
        }
    }

    // ── Merge ────────────────────────────────────────────────────────────

    fun mergePullRequest(method: MergePullRequestOption.Do, deleteBranch: Boolean) {
        LOG.info("PR #$prNumber: merging ($method, delete branch $deleteBranch)")
        cs.launch(Dispatchers.IO) {
            _isActionInProgress.value = true
            try {
                repository.mergePullRequest(prNumber, MergePullRequestOption(`do` = method, deleteBranchAfterMerge = deleteBranch))
                _pr.value = repository.loadPullRequest(prNumber)
                LOG.info("PR #$prNumber: merged ${_pr.value.merged}")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                notifyError("pull.request.action.merge.error", e)
            } finally {
                withContext(NonCancellable) { _isActionInProgress.value = false }
            }
        }
    }

    // ── Request review ───────────────────────────────────────────────────

    /** Requested reviewers as the picker shows them — without the author, whom Gitea also lists
     * here once they've commented in a review, but who can't be requested (see [loadPossibleReviewers]). */
    val currentlyRequestedReviewers: List<GiteaUser>
        get() = _pr.value.requestedReviewers.filterNot { it.login.equals(_pr.value.author.login, ignoreCase = true) }

    /** Candidate reviewers for the Request Review picker — collaborators, or every user on the
     * instance, per the signed-in account's "list all users" setting. */
    /** Everyone who can be asked for a review, except the PR's author (Gitea rejects that with
     * "poster of pr can't be reviewer", failing the whole update). */
    suspend fun loadPossibleReviewers(): List<GiteaUser> =
        repository.loadPossibleReviewers(GiteaSettings.getInstance().allUsersArePotentialReviewers)
            .filterNot { it.login.equals(_pr.value.author.login, ignoreCase = true) }

    /**
     * Applies a reviewer picker's [delta] (add/remove requested reviewers) and reloads the PR —
     * [GiteaPRStatusViewModel.reviewerStates] observes [prFlow] and refreshes on its own once
     * `requestedReviewers` changes, so no extra plumbing is needed here.
     */
    fun requestReview(newLogins: List<String>, removedLogins: List<String>) {
        LOG.info("PR #$prNumber: updating requested reviewers (add $newLogins, remove $removedLogins)")
        cs.launch(Dispatchers.IO) {
            _isActionInProgress.value = true
            try {
                repository.requestReviewers(prNumber, newLogins)
                repository.removeReviewRequest(prNumber, removedLogins)
                _pr.value = repository.loadPullRequest(prNumber)
                LOG.info("PR #$prNumber: requested reviewers now ${_pr.value.requestedReviewers.map { it.login }}")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                notifyError("pull.request.action.request.review.error", e)
            } finally {
                withContext(NonCancellable) { _isActionInProgress.value = false }
            }
        }
    }

    /** An error notification titled by [bundleKey], with [cause]'s message (Gitea's own, for API
     * failures) as its text. */
    private suspend fun notifyError(bundleKey: String, cause: Throwable? = null) {
        LOG.warn("PR #$prNumber: ${GiteaBundle.message(bundleKey)}", cause)
        withContext(Dispatchers.Main) {
            NotificationGroupManager.getInstance()
                .getNotificationGroup("Gitea")
                .createNotification(GiteaBundle.message(bundleKey), cause?.localizedMessage.orEmpty(), NotificationType.ERROR)
                .notify(project)
        }
    }

    init {
        // The PR handed in usually comes from the list, which may be older than what's on the
        // server now (e.g. a conflict fixed by a push), so show it and fetch a fresh copy.
        refresh()
    }

    private companion object {
        /** Gitea's two default work-in-progress title prefixes (`repository.pull-request.WORK_IN_PROGRESS_PREFIXES`). */
        private val DRAFT_PREFIX_RE = Regex("""^\s*(WIP:|\[WIP])\s*""", RegexOption.IGNORE_CASE)
    }
}
