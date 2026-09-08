package com.github.jpmand.idea.plugin.gitea.pullrequest.ui.details

import com.github.jpmand.idea.plugin.gitea.api.models.GiteaPullRequest
import com.github.jpmand.idea.plugin.gitea.api.models.GiteaUser
import com.github.jpmand.idea.plugin.gitea.api.rest.dto.CommitStatus
import com.github.jpmand.idea.plugin.gitea.pullrequest.data.GiteaPRRepository
import com.github.jpmand.idea.plugin.gitea.pullrequest.ui.list.GiteaPRReviewerState
import com.github.jpmand.idea.plugin.gitea.pullrequest.ui.list.computeReviewerStates
import com.intellij.collaboration.ui.codereview.details.data.CodeReviewCIJob
import com.intellij.collaboration.ui.codereview.details.data.CodeReviewCIJobState
import com.intellij.collaboration.ui.codereview.details.data.ReviewState
import com.intellij.collaboration.ui.codereview.details.model.CodeReviewStatusViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Feeds the read-only "status" section of the PR details view (CI checks, missing-reviewer
 * nudge, merge-conflict banner) via [com.intellij.collaboration.ui.codereview.details.CodeReviewDetailsStatusComponentFactory].
 *
 * Sourced from data [GiteaPRRepository] already exposes — no new REST endpoints needed.
 */
@Suppress("UnstableApiUsage")
class GiteaPRStatusViewModel(
    private val cs: CoroutineScope,
    private val initialPr: GiteaPullRequest,
    private val repository: GiteaPRRepository,
) : CodeReviewStatusViewModel {

    // Same heuristic as the list's mergeable-conflict icon (Gitea only exposes a boolean
    // "mergeable" flag, no GitHub-style tri-state) — see GiteaPRListPanel for the rationale.
    private val _hasConflicts = MutableStateFlow(
        !initialPr.mergeable && initialPr.state == "open" && !initialPr.merged && !initialPr.draft
    )
    override val hasConflicts: SharedFlow<Boolean> = _hasConflicts.asStateFlow()

    private val _ciJobs = MutableStateFlow<List<CodeReviewCIJob>>(emptyList())
    override val ciJobs: SharedFlow<List<CodeReviewCIJob>> = _ciJobs.asStateFlow()

    private val _showJobsDetailsRequests = MutableSharedFlow<List<CodeReviewCIJob>>()
    override val showJobsDetailsRequests: SharedFlow<List<CodeReviewCIJob>> = _showJobsDetailsRequests.asSharedFlow()

    private val _reviewerStates = MutableStateFlow<Map<GiteaUser, ReviewState>>(emptyMap())
    val reviewerStates: StateFlow<Map<GiteaUser, ReviewState>> = _reviewerStates.asStateFlow()

    init {
        cs.launch(Dispatchers.IO) {
            try {
                val combined = repository.loadCombinedStatus(initialPr.head.sha)
                _ciJobs.value = combined.statuses.orEmpty().map { it.toCiJob() }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _ciJobs.value = emptyList()
            }
        }
        cs.launch(Dispatchers.IO) {
            try {
                val reviews = repository.loadReviews(initialPr.number.toInt())
                val states = computeReviewerStates(initialPr.requestedReviewers, reviews)
                _reviewerStates.value = states.mapValues { (_, state) -> state.toReviewState() }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _reviewerStates.value = emptyMap()
            }
        }
    }

    override fun showJobsDetails() {
        cs.launch { _showJobsDetailsRequests.emit(_ciJobs.value) }
    }

    private fun CommitStatus.toCiJob(): CodeReviewCIJob = CodeReviewCIJob(
        name = context ?: description ?: "check",
        status = when (status) {
            CommitStatus.Status.PENDING, CommitStatus.Status.WARNING -> CodeReviewCIJobState.PENDING
            CommitStatus.Status.SUCCESS -> CodeReviewCIJobState.SUCCESS
            CommitStatus.Status.ERROR, CommitStatus.Status.FAILURE -> CodeReviewCIJobState.FAILED
            CommitStatus.Status.SKIPPED -> CodeReviewCIJobState.SKIPPED
            null -> CodeReviewCIJobState.PENDING
        },
        isRequired = false, // Gitea's commit-status API doesn't expose branch-protection "required" flags
        detailsUrl = targetUrl ?: initialPr.htmlUrl,
    )

    private fun GiteaPRReviewerState.toReviewState(): ReviewState = when (this) {
        GiteaPRReviewerState.APPROVED -> ReviewState.ACCEPTED
        GiteaPRReviewerState.CHANGES_REQUESTED -> ReviewState.WAIT_FOR_UPDATES
        GiteaPRReviewerState.COMMENTED, GiteaPRReviewerState.NEEDS_REVIEW -> ReviewState.NEED_REVIEW
    }
}
