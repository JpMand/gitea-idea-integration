package com.github.jpmand.idea.plugin.gitea.pullrequest.review

import com.github.jpmand.idea.plugin.gitea.api.rest.models.pr.GiteaCreatePullRequestReviewCommentDTO
import com.github.jpmand.idea.plugin.gitea.api.rest.models.pr.GiteaCreatePullRequestReviewRequestDTO
import com.github.jpmand.idea.plugin.gitea.api.rest.models.pr.GiteaReviewEventEnum
import com.github.jpmand.idea.plugin.gitea.pullrequest.data.GiteaPRRepository
import com.intellij.collaboration.ui.codereview.review.CodeReviewSubmitViewModel
import com.intellij.collaboration.util.SingleCoroutineLauncher
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext

/**
 * Per-popup ViewModel for the review submission popup.
 *
 * Created fresh on each "Submit Review" button click. Its [CoroutineScope] is tied to the
 * coroutine that calls [GiteaPRSubmitReviewPopupHandler.show], so it is automatically cleaned up
 * when the popup closes (either by the user or by a successful/cancelled submission).
 *
 * [onDone] is called by [submit] on success and by [cancel] to programmatically close the popup.
 */
@Suppress("UnstableApiUsage")
class GiteaPRReviewViewModel(
    cs: CoroutineScope,
    private val prNumber: Int,
    private val headSha: String,
    private val repository: GiteaPRRepository,
    private val discussionsVm: GiteaPRDiscussionsViewModels,
    private val onDone: () -> Unit,
) : CodeReviewSubmitViewModel {

    private val taskLauncher = SingleCoroutineLauncher(cs)

    override val draftCommentsCount: StateFlow<Int> = discussionsVm.draftComments
        .map { it.size }
        .stateIn(cs, SharingStarted.Eagerly, 0)

    override val text: MutableStateFlow<String> = MutableStateFlow("")

    override val isBusy: StateFlow<Boolean> = taskLauncher.busy

    override val error: MutableStateFlow<Throwable?> = MutableStateFlow(null)

    /**
     * Submits the review: sends the body text and all accumulated draft comments to the API
     * with the given [event] (Approved / Request Changes / Comment), then clears the drafts
     * and calls [onDone] to close the popup.
     *
     * Uses [SingleCoroutineLauncher] — a second call while a submit is in progress is silently
     * dropped (no double-submit).
     */
    fun submit(event: GiteaReviewEventEnum) {
        taskLauncher.launch {
            error.value = null
            try {
                // Snapshot before the network call so we remove exactly what was submitted.
                val drafts = discussionsVm.draftComments.value
                val comments = drafts.map { draft ->
                    GiteaCreatePullRequestReviewCommentDTO(
                        path = draft.path,
                        body = draft.body,
                        newPosition = draft.newPosition ?: 0,
                        oldPosition = draft.oldPosition ?: 0,
                    )
                }
                val request = GiteaCreatePullRequestReviewRequestDTO(
                    body = text.value.takeIf { it.isNotBlank() },
                    commitId = headSha,
                    event = event,
                    comments = comments.ifEmpty { null },
                )
                withContext(Dispatchers.IO) {
                    repository.submitReview(prNumber, request)
                }
                for (draft in drafts) {
                    discussionsVm.removeDraftComment(draft.localId)
                }
                discussionsVm.reload()
                onDone()
            } catch (ce: CancellationException) {
                throw ce
            } catch (e: Exception) {
                error.value = e
            }
        }
    }

    override fun cancel() = onDone()
}
