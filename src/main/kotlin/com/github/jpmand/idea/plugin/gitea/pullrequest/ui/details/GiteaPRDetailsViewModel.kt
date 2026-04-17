package com.github.jpmand.idea.plugin.gitea.pullrequest.ui.details

import com.github.jpmand.idea.plugin.gitea.api.models.GiteaPullRequest
import com.github.jpmand.idea.plugin.gitea.api.rest.models.pr.GiteaEditPullRequestRequestDTO
import com.github.jpmand.idea.plugin.gitea.api.rest.models.pr.GiteaMergePullRequestRequestDTO
import com.github.jpmand.idea.plugin.gitea.api.rest.models.pr.GiteaMergeStyleEnum
import com.github.jpmand.idea.plugin.gitea.pullrequest.data.GiteaPRRepository
import com.intellij.collaboration.ui.codereview.details.data.ReviewRequestState
import com.intellij.collaboration.ui.codereview.details.model.CodeReviewDetailsViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Suppress("UnstableApiUsage")
class GiteaPRDetailsViewModel(
    private val cs: CoroutineScope,
    initialPr: GiteaPullRequest,
    private val repository: GiteaPRRepository,
) : CodeReviewDetailsViewModel {

    private val _pr = MutableStateFlow(initialPr)

    val prNumber: Int = initialPr.number

    override val number: String = "#${initialPr.number}"
    override val url: String = initialPr.htmlUrl

    override val title: Flow<String> = _pr.map { it.title }

    /** Null when the initial PR has no body — skips the description pane entirely. */
    override val description: Flow<String>? =
        if (initialPr.body.isNullOrBlank()) null
        else _pr.map { it.body ?: "" }

    override val reviewRequestState: Flow<ReviewRequestState> = _pr.map { pr ->
        when {
            pr.merged -> ReviewRequestState.MERGED
            pr.state == "closed" -> ReviewRequestState.CLOSED
            pr.draft -> ReviewRequestState.DRAFT
            else -> ReviewRequestState.OPENED
        }
    }

    val branchesVm = GiteaPRBranchesViewModel(cs, _pr)
    val changesVm = GiteaPRChangesViewModel(cs, prNumber, repository)

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

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

    fun merge(style: GiteaMergeStyleEnum = GiteaMergeStyleEnum.MERGE) {
        cs.launch(Dispatchers.IO) {
            _error.value = null
            try {
                repository.mergePullRequest(prNumber, GiteaMergePullRequestRequestDTO(style))
                _pr.value = repository.loadPullRequest(prNumber)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _error.value = e
            }
        }
    }

    fun close() {
        cs.launch(Dispatchers.IO) {
            _error.value = null
            try {
                _pr.value = repository.editPullRequest(prNumber, GiteaEditPullRequestRequestDTO(state = "closed"))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _error.value = e
            }
        }
    }

    fun reopen() {
        cs.launch(Dispatchers.IO) {
            _error.value = null
            try {
                _pr.value = repository.editPullRequest(prNumber, GiteaEditPullRequestRequestDTO(state = "open"))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _error.value = e
            }
        }
    }
}
