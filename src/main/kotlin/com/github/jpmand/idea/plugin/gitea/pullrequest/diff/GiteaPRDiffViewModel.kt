package com.github.jpmand.idea.plugin.gitea.pullrequest.diff

import com.github.jpmand.idea.plugin.gitea.api.models.GiteaPullRequest
import com.github.jpmand.idea.plugin.gitea.pullrequest.data.GiteaPRRepository
import com.intellij.collaboration.ui.codereview.diff.model.CodeReviewDiffProcessorViewModel
import com.intellij.collaboration.ui.codereview.diff.model.DiffViewerScrollRequest
import com.intellij.collaboration.util.ComputedResult
import com.intellij.openapi.ListSelection
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Suppress("UnstableApiUsage")
class GiteaPRDiffViewModel(
    parentCs: CoroutineScope,
    private val project: Project,
    private val pr: GiteaPullRequest,
    private val repository: GiteaPRRepository,
) : CodeReviewDiffProcessorViewModel<GiteaPRDiffFileViewModel> {

    private val cs = CoroutineScope(parentCs.coroutineContext + SupervisorJob(parentCs.coroutineContext[Job]))

    private val _changesState =
        MutableStateFlow<ComputedResult<CodeReviewDiffProcessorViewModel.State<GiteaPRDiffFileViewModel>>?>(null)
    override val changes: StateFlow<ComputedResult<CodeReviewDiffProcessorViewModel.State<GiteaPRDiffFileViewModel>>?> =
        _changesState.asStateFlow()

    init {
        cs.launch(Dispatchers.IO) {
            _changesState.value = ComputedResult.loading()
            try {
                val files = repository.loadChangedFiles(pr.number)
                val fileVms = files.map { file ->
                    GiteaPRDiffFileViewModel(cs, project, repository, file, pr.base.sha, pr.head.sha)
                }
                withContext(Dispatchers.Main) {
                    _changesState.value = ComputedResult.success(
                        SimpleState(ListSelection.createAt(fileVms, if (fileVms.isEmpty()) -1 else 0))
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _changesState.value = ComputedResult.failure(e)
            }
        }
    }

    override fun showChange(change: GiteaPRDiffFileViewModel, scrollRequest: DiffViewerScrollRequest?) {
        val current = _changesState.value?.result?.getOrNull() ?: return
        val idx = current.selectedChanges.list.indexOf(change)
        if (idx >= 0) {
            _changesState.value = ComputedResult.success(
                SimpleState(ListSelection.createAt(current.selectedChanges.list, idx))
            )
        }
    }

    override fun showChange(changeIdx: Int, scrollRequest: DiffViewerScrollRequest?) {
        val current = _changesState.value?.result?.getOrNull() ?: return
        if (changeIdx in current.selectedChanges.list.indices) {
            _changesState.value = ComputedResult.success(
                SimpleState(ListSelection.createAt(current.selectedChanges.list, changeIdx))
            )
        }
    }

    private class SimpleState(
        override val selectedChanges: ListSelection<GiteaPRDiffFileViewModel>,
    ) : CodeReviewDiffProcessorViewModel.State<GiteaPRDiffFileViewModel>
}
