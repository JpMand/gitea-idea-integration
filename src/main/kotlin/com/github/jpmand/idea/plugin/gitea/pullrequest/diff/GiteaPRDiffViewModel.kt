package com.github.jpmand.idea.plugin.gitea.pullrequest.diff

import com.github.jpmand.idea.plugin.gitea.api.models.GiteaPullRequest
import com.github.jpmand.idea.plugin.gitea.pullrequest.data.GiteaPRRepository
import com.intellij.collaboration.ui.codereview.diff.model.CodeReviewDiffProcessorViewModel
import com.intellij.collaboration.ui.codereview.diff.model.DiffViewerScrollRequest
import com.intellij.collaboration.util.ComputedResult
import com.intellij.openapi.ListSelection
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val LOG = logger<GiteaPRDiffViewModel>()

/**
 * The PR's changed files for the diff viewer, compared from [GiteaPullRequest.diffBaseSha] to the
 * head. Reloads whenever [prFlow] brings a new head or merge base (e.g. after a push and a
 * refresh), keeping the selected file when it's still part of the PR.
 */
@Suppress("UnstableApiUsage")
class GiteaPRDiffViewModel(
    parentCs: CoroutineScope,
    private val project: Project,
    prFlow: StateFlow<GiteaPullRequest>,
    private val repository: GiteaPRRepository,
) : CodeReviewDiffProcessorViewModel<GiteaPRDiffFileViewModel> {

    private val cs = CoroutineScope(parentCs.coroutineContext + SupervisorJob(parentCs.coroutineContext[Job]))

    private val _changesState =
        MutableStateFlow<ComputedResult<CodeReviewDiffProcessorViewModel.State<GiteaPRDiffFileViewModel>>?>(null)
    override val changes: StateFlow<ComputedResult<CodeReviewDiffProcessorViewModel.State<GiteaPRDiffFileViewModel>>?> =
        _changesState.asStateFlow()

    init {
        cs.launch {
            prFlow.distinctUntilChangedBy { Triple(it.number, it.diffBaseSha, it.head.sha) }.collectLatest { pr ->
                // The file view models of the previous load live only as long as it does.
                coroutineScope {
                    val previous = _changesState.value?.result?.getOrNull()?.selectedChanges
                    _changesState.value = ComputedResult.loading()
                    try {
                        val files = withContext(Dispatchers.IO) { repository.loadChangedFiles(pr.number.toInt()) }
                        LOG.debug("PR #${pr.number}: diff has ${files.size} changed files (${pr.diffBaseSha}..${pr.head.sha})")
                        val fileVms = files.map { file ->
                            GiteaPRDiffFileViewModel(this, project, repository, file, pr.diffBaseSha, pr.head.sha)
                        }
                        val previousName = previous?.let { it.list.getOrNull(it.selectedIndex)?.file?.filename }
                        val selected = fileVms.indexOfFirst { it.file.filename == previousName }
                            .takeIf { it >= 0 } ?: if (fileVms.isEmpty()) -1 else 0
                        withContext(Dispatchers.Main) {
                            _changesState.value = ComputedResult.success(SimpleState(ListSelection.createAt(fileVms, selected)))
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        LOG.warn("PR #${pr.number}: couldn't load the changed files for the diff", e)
                        _changesState.value = ComputedResult.failure(e)
                    }
                    awaitCancellation()
                }
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
