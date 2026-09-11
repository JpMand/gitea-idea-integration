package com.github.jpmand.idea.plugin.gitea.pullrequest.ui.details

import com.github.jpmand.idea.plugin.gitea.api.models.GiteaPullRequest
import com.github.jpmand.idea.plugin.gitea.pullrequest.data.GiteaPRRepository
import com.intellij.collaboration.ui.codereview.details.data.ReviewRequestState
import com.intellij.collaboration.ui.codereview.details.model.CodeReviewDetailsViewModel
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import com.intellij.openapi.util.text.StringUtil
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Milestone-1 (read-only) details view model: title/description/status/branches/commits.
 * Mutating actions (merge/close/reopen) are intentionally not exposed here — Milestone 2.
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

    override val title: Flow<String> = _pr.map { it.title }

    /**
     * Null when the initial PR has no body — skips the description pane entirely. Otherwise
     * emits an escaped-plain-text fallback immediately, then the server-rendered markdown HTML
     * once it's back (or never, if rendering fails — the fallback stays).
     */
    override val description: Flow<String>? =
        if (initialPr.body.isNullOrBlank()) null
        else _pr.map { it.body ?: "" }.transformLatest { markdown ->
            emit(escapedFallback(markdown))
            repository.renderMarkdown(markdown)?.let { emit(it) }
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

    private fun escapedFallback(markdown: String): String =
        StringUtil.escapeXmlEntities(markdown).replace("\n", "<br>")
}
