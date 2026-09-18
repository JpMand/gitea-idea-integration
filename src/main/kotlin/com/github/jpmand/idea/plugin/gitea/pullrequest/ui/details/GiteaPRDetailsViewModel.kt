package com.github.jpmand.idea.plugin.gitea.pullrequest.ui.details

import com.github.jpmand.idea.plugin.gitea.api.models.GiteaPullRequest
import com.github.jpmand.idea.plugin.gitea.api.rest.dto.EditPullRequestOption
import com.github.jpmand.idea.plugin.gitea.api.rest.dto.MergePullRequestOption
import com.github.jpmand.idea.plugin.gitea.pullrequest.data.GiteaPRRepository
import com.github.jpmand.idea.plugin.gitea.util.GiteaBundle
import com.github.jpmand.idea.plugin.gitea.util.GiteaUtil
import com.intellij.collaboration.ui.codereview.details.data.ReviewRequestState
import com.intellij.collaboration.ui.codereview.details.model.CodeReviewDetailsViewModel
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

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

    val prNumber: Int = initialPr.number.toInt()

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

    fun refresh() {
        cs.launch(Dispatchers.IO) {
            _isLoading.value = true
            _error.value = null
            try {
                _pr.value = repository.loadPullRequest(prNumber)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
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
        cs.launch(Dispatchers.IO) {
            _isActionInProgress.value = true
            try {
                _pr.value = repository.editPullRequest(prNumber, EditPullRequestOption(state = state))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                notifyError(errorKey)
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
        cs.launch(Dispatchers.IO) {
            _isActionInProgress.value = true
            try {
                _pr.value = repository.editPullRequest(prNumber, EditPullRequestOption(title = newTitle))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                notifyError("pull.request.action.ready.for.review.error")
            } finally {
                withContext(NonCancellable) { _isActionInProgress.value = false }
            }
        }
    }

    // ── Merge ────────────────────────────────────────────────────────────

    fun mergePullRequest(method: MergePullRequestOption.Do, deleteBranch: Boolean) {
        cs.launch(Dispatchers.IO) {
            _isActionInProgress.value = true
            try {
                repository.mergePullRequest(prNumber, MergePullRequestOption(`do` = method, deleteBranchAfterMerge = deleteBranch))
                _pr.value = repository.loadPullRequest(prNumber)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                notifyError("pull.request.action.merge.error")
            } finally {
                withContext(NonCancellable) { _isActionInProgress.value = false }
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

    private companion object {
        /** Gitea's two default work-in-progress title prefixes (`repository.pull-request.WORK_IN_PROGRESS_PREFIXES`). */
        private val DRAFT_PREFIX_RE = Regex("""^\s*(WIP:|\[WIP])\s*""", RegexOption.IGNORE_CASE)
    }
}
