package com.github.jpmand.idea.plugin.gitea.pullrequest.ui.details

import com.github.jpmand.idea.plugin.gitea.api.models.GiteaCommitStatus
import com.github.jpmand.idea.plugin.gitea.api.models.GiteaCommitStatusState
import com.github.jpmand.idea.plugin.gitea.api.models.GiteaPullRequest
import com.github.jpmand.idea.plugin.gitea.api.models.GiteaUser
import com.github.jpmand.idea.plugin.gitea.pullrequest.data.GiteaPRRepository
import com.github.jpmand.idea.plugin.gitea.pullrequest.ui.list.computeReviewerStates
import com.github.jpmand.idea.plugin.gitea.pullrequest.ui.list.toReviewState
import com.intellij.collaboration.ui.codereview.details.data.CodeReviewCIJob
import com.intellij.collaboration.ui.codereview.details.data.CodeReviewCIJobState
import com.intellij.collaboration.ui.codereview.details.data.ReviewState
import com.intellij.collaboration.ui.codereview.details.model.CodeReviewStatusViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
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
    private val prFlow: StateFlow<GiteaPullRequest>,
    private val localMergeability: StateFlow<Boolean?>,
    private val repository: GiteaPRRepository,
) : CodeReviewStatusViewModel {

    private val initialPr: GiteaPullRequest get() = prFlow.value

    // Reactive so a merge/close/refresh (or a newly-completed local mergeability check) updates
    // the banner and the merge-button gate without a manual reload. Gitea only exposes a boolean
    // "mergeable" flag (see GiteaPRListPanel for the rationale) — ORed with an independent local
    // git merge-tree check (GiteaPRBranchesViewModel.localMergeabilityState) against the *remote*
    // target branch, since Gitea's own flag can be too coarse.
    override val hasConflicts: SharedFlow<Boolean> = combine(prFlow, localMergeability) { pr, local ->
        val relevant = pr.state == "open" && !pr.merged && !pr.draft
        relevant && (!pr.mergeable || local == false)
    }.stateIn(cs, SharingStarted.Eagerly, false)

    private val _ciJobs = MutableStateFlow<List<CodeReviewCIJob>>(emptyList())
    override val ciJobs: SharedFlow<List<CodeReviewCIJob>> = _ciJobs.asStateFlow()

    private val _showJobsDetailsRequests = MutableSharedFlow<List<CodeReviewCIJob>>()
    override val showJobsDetailsRequests: SharedFlow<List<CodeReviewCIJob>> = _showJobsDetailsRequests.asSharedFlow()

    private val _reviewerStates = MutableStateFlow<Map<GiteaUser, ReviewState>>(emptyMap())
    val reviewerStates: StateFlow<Map<GiteaUser, ReviewState>> = _reviewerStates.asStateFlow()

    init {
        cs.launch(Dispatchers.IO) {
            try {
                val statuses = repository.loadCombinedStatus(initialPr.head.sha)
                _ciJobs.value = statuses.map { it.toCiJob() }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _ciJobs.value = emptyList()
            }
        }
        // Re-runs whenever the requested-reviewers list itself changes (e.g. after Request
        // Review) — collectLatest so a fast follow-up change cancels a stale in-flight reload.
        cs.launch(Dispatchers.IO) {
            prFlow.map { it.requestedReviewers }.distinctUntilChanged().collectLatest { requestedReviewers ->
                try {
                    val reviews = repository.loadReviews(initialPr.number.toInt())
                    val states = computeReviewerStates(requestedReviewers, reviews, initialPr.author.login)
                    _reviewerStates.value = states.mapValues { (_, state) -> state.toReviewState() }
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    _reviewerStates.value = emptyMap()
                }
            }
        }
    }

    override fun showJobsDetails() {
        cs.launch { _showJobsDetailsRequests.emit(_ciJobs.value) }
    }

    private fun GiteaCommitStatus.toCiJob(): CodeReviewCIJob {
        val state = when (status) {
            GiteaCommitStatusState.PENDING, GiteaCommitStatusState.WARNING -> CodeReviewCIJobState.PENDING
            GiteaCommitStatusState.SUCCESS -> CodeReviewCIJobState.SUCCESS
            GiteaCommitStatusState.ERROR, GiteaCommitStatusState.FAILURE -> CodeReviewCIJobState.FAILED
            GiteaCommitStatusState.SKIPPED -> CodeReviewCIJobState.SKIPPED
            GiteaCommitStatusState.UNKNOWN -> CodeReviewCIJobState.PENDING
        }
        return CodeReviewCIJob(
            name = context ?: description ?: "check",
            status = state,
            // Gitea's commit-status API doesn't say which checks branch protection requires, so
            // every non-skipped check counts (as the GitLab plugin does). With none required, the
            // platform reports "All required checks have passed" even while checks are pending.
            isRequired = state != CodeReviewCIJobState.SKIPPED,
            detailsUrl = targetUrl ?: initialPr.htmlUrl,
        )
    }
}
