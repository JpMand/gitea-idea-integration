package com.github.jpmand.idea.plugin.gitea.pullrequest.ui.timeline

import com.github.jpmand.idea.plugin.gitea.api.models.GiteaPullRequest
import com.github.jpmand.idea.plugin.gitea.api.models.GiteaReview
import com.github.jpmand.idea.plugin.gitea.api.rest.dto.CreatePullReviewOptions
import com.github.jpmand.idea.plugin.gitea.api.rest.dto.SubmitPullReviewOptions
import com.github.jpmand.idea.plugin.gitea.pullrequest.data.GiteaPRRepository
import com.github.jpmand.idea.plugin.gitea.pullrequest.ui.comment.GiteaPRSubmittableTextViewModel
import com.github.jpmand.idea.plugin.gitea.util.GiteaBundle
import com.intellij.collaboration.util.ComputedResult
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date

/** Read-only view model for a PR's activity timeline (Conversation). */
class GiteaPRTimelineViewModel(
    parentCs: CoroutineScope,
    private val project: Project,
    val pr: GiteaPullRequest,
    private val repository: GiteaPRRepository,
) {

    private val cs = CoroutineScope(parentCs.coroutineContext + SupervisorJob(parentCs.coroutineContext[Job]))

    val number: String = "#${pr.number}"
    val title: String = pr.title
    val descriptionMarkdown: String? = pr.body?.takeIf { it.isNotBlank() }
    val author = pr.author
    val createdAt: Date = pr.createdAt

    /** Posts a new top-level timeline comment, then reloads the timeline to show it. */
    val newCommentVm = GiteaPRSubmittableTextViewModel(project, cs) { body ->
        repository.createComment(pr.number.toInt(), body)
        reload()
    }

    private val _items = MutableStateFlow<ComputedResult<List<GiteaPRTimelineItemViewModel>>?>(null)
    val items: StateFlow<ComputedResult<List<GiteaPRTimelineItemViewModel>>?> = _items.asStateFlow()

    /** The signed-in account's own not-yet-submitted review for this PR, if any — surfaced as a
     * "finish your review" prompt instead of the "start a review" composer. */
    private val _pendingReview = MutableStateFlow<GiteaReview?>(null)
    val pendingReview: StateFlow<GiteaReview?> = _pendingReview.asStateFlow()

    private val _isSubmittingReview = MutableStateFlow(false)
    val isSubmittingReview: StateFlow<Boolean> = _isSubmittingReview.asStateFlow()

    private var loadJob: Job? = null

    init {
        reload()
        reloadPendingReview()
    }

    fun reload() {
        loadJob?.cancel()
        loadJob = cs.launch(Dispatchers.IO) {
            _items.value = ComputedResult.loading()
            try {
                val items = repository.loadTimeline(pr.number.toInt()).toItemViewModels()
                _items.value = ComputedResult.success(items)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _items.value = ComputedResult.failure(e)
            }
        }
    }

    private fun reloadPendingReview() {
        cs.launch(Dispatchers.IO) {
            try {
                _pendingReview.value = repository.findMyPendingReview(pr.number.toInt())
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Best-effort — a failed lookup just means no "finish your review" prompt shows.
            }
        }
    }

    /**
     * Submits a brand-new review with the given verdict — or, when [event] is `PENDING`, creates
     * a draft review that only becomes visible to others once [submitPendingReview] finishes it.
     * A pending review created this way is body-only: there's no per-line diff-comment
     * composition (that's a separate, larger feature — see the plan notes).
     */
    fun submitReview(event: CreatePullReviewOptions.Event, body: String) {
        cs.launch(Dispatchers.IO) {
            _isSubmittingReview.value = true
            try {
                repository.submitReview(pr.number.toInt(), CreatePullReviewOptions(body = body.ifBlank { null }, event = event))
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

    /** Finishes (submits) the currently pending review with the given verdict. */
    fun submitPendingReview(event: SubmitPullReviewOptions.Event, body: String) {
        val reviewId = pendingReview.value?.id ?: return
        cs.launch(Dispatchers.IO) {
            _isSubmittingReview.value = true
            try {
                repository.submitPendingReview(
                    pr.number.toInt(), reviewId,
                    SubmitPullReviewOptions(body = body.ifBlank { null }, event = event),
                )
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

    private suspend fun notifyError(bundleKey: String) {
        withContext(Dispatchers.Main) {
            NotificationGroupManager.getInstance()
                .getNotificationGroup("Gitea")
                .createNotification(GiteaBundle.message(bundleKey), NotificationType.ERROR)
                .notify(project)
        }
    }
}
