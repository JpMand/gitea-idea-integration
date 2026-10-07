package com.github.jpmand.idea.plugin.gitea.pullrequest.diff

import com.github.jpmand.idea.plugin.gitea.api.models.GiteaPullRequest
import com.github.jpmand.idea.plugin.gitea.pullrequest.data.GiteaPRRepository
import com.intellij.collaboration.ui.codereview.diff.model.CodeReviewDiffProcessorViewModel
import com.intellij.collaboration.ui.codereview.diff.model.DiffViewerScrollRequest
import com.intellij.collaboration.util.ComputedResult
import com.intellij.openapi.ListSelection
import com.intellij.openapi.application.EDT
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val LOG = logger<GiteaPRDiffViewModel>()

/**
 * The changed files of one [GiteaPRDiffTarget] of the PR for the diff viewer: by default the whole
 * PR, compared from [GiteaPullRequest.diffBaseSha] to the head; [selectFile] switches to another
 * target, e.g. a single commit or the PR as a review saw it. Reloads whenever [prFlow] brings a new head or merge base (e.g.
 * after a push and a refresh), keeping the selected file when it's still part of the diff.
 */
@Suppress("UnstableApiUsage")
class GiteaPRDiffViewModel(
    parentCs: CoroutineScope,
    private val project: Project,
    prFlow: StateFlow<GiteaPullRequest>,
    private val repository: GiteaPRRepository,
) : CodeReviewDiffProcessorViewModel<GiteaPRDiffFileViewModel> {

    private val cs = CoroutineScope(parentCs.coroutineContext + SupervisorJob(parentCs.coroutineContext[Job]))

    private val target = MutableStateFlow<GiteaPRDiffTarget>(GiteaPRDiffTarget.PullRequest)

    private val _changesState =
        MutableStateFlow<ComputedResult<CodeReviewDiffProcessorViewModel.State<GiteaPRDiffFileViewModel>>?>(null)
    override val changes: StateFlow<ComputedResult<CodeReviewDiffProcessorViewModel.State<GiteaPRDiffFileViewModel>>?> =
        _changesState.asStateFlow()

    init {
        cs.launch {
            combine(
                prFlow.distinctUntilChangedBy { Triple(it.number, it.diffBaseSha, it.head.sha) },
                target,
            ) { pr, target -> pr to target }.collectLatest { (pr, target) ->
                // The file view models of the previous load live only as long as it does.
                coroutineScope {
                    val previous = (_changesState.value?.result?.getOrNull() as? SimpleState)
                        ?.takeIf { it.target == target }?.selectedChanges
                    _changesState.value = ComputedResult.loading()
                    try {
                        val (baseSha, headSha) = when (target) {
                            GiteaPRDiffTarget.PullRequest -> pr.diffBaseSha to pr.head.sha
                            is GiteaPRDiffTarget.Commit -> target.parentSha to target.sha
                            is GiteaPRDiffTarget.PullRequestAt -> pr.diffBaseSha to target.sha
                        }
                        val files = withContext(Dispatchers.IO) {
                            when (target) {
                                GiteaPRDiffTarget.PullRequest -> repository.loadChangedFiles(pr.number.toInt())
                                is GiteaPRDiffTarget.Commit -> repository.loadCommitChangedFiles(target.sha)
                                is GiteaPRDiffTarget.PullRequestAt -> repository.loadComparedFiles(pr.diffBaseSha, target.sha)
                            }
                        }
                        LOG.debug("PR #${pr.number}: diff of $target has ${files.size} changed files ($baseSha..$headSha)")
                        val fileVms = files.map { file ->
                            GiteaPRDiffFileViewModel(this, project, repository, file, target, pr.diffBaseSha, baseSha, headSha)
                        }
                        val previousName = previous?.let { it.list.getOrNull(it.selectedIndex)?.file?.filename }
                        val selected = fileVms.indexOfFirst { it.file.filename == previousName }
                            .takeIf { it >= 0 } ?: if (fileVms.isEmpty()) -1 else 0
                        withContext(Dispatchers.Main) {
                            _changesState.value = ComputedResult.success(SimpleState(target, ListSelection.createAt(fileVms, selected)))
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        LOG.warn("PR #${pr.number}: couldn't load the changed files of $target for the diff", e)
                        _changesState.value = ComputedResult.failure(e)
                    }
                    awaitCancellation()
                }
            }
        }
    }

    /**
     * Switches the diff to [newTarget], waits for its files and selects [path] (a file's current
     * name or, for a renamed file, its old one), scrolled by [scrollRequest]. Returns `false` when
     * [path] isn't part of that diff, `true` otherwise — also when the files failed to load, so the
     * diff opens on the error, which offers a reload.
     */
    suspend fun selectFile(newTarget: GiteaPRDiffTarget, path: String, scrollRequest: DiffViewerScrollRequest?): Boolean {
        val result = withContext(Dispatchers.EDT) {
            if (target.value != newTarget) {
                // Drop the old target's result first, so it isn't mistaken for the new one's.
                _changesState.value = ComputedResult.loading()
                target.value = newTarget
            }
            changes.first { state ->
                val loaded = state?.result ?: return@first false
                loaded.isFailure || (loaded.getOrNull() as? SimpleState)?.target == newTarget
            }!!.result!!
        }
        val files = result.getOrNull()?.selectedChanges?.list ?: return true
        val idx = files.indexOfFirst { it.file.filename == path || it.file.previousFilename == path }
        if (idx < 0) {
            LOG.debug("$path is not among the ${files.size} changed files of the diff of $newTarget")
            return false
        }
        withContext(Dispatchers.EDT) { showChange(idx, scrollRequest) }
        return true
    }

    override fun showChange(change: GiteaPRDiffFileViewModel, scrollRequest: DiffViewerScrollRequest?) {
        val current = _changesState.value?.result?.getOrNull() as? SimpleState ?: return
        val idx = current.selectedChanges.list.indexOf(change)
        if (idx >= 0) {
            _changesState.value = ComputedResult.success(
                SimpleState(current.target, ListSelection.createAt(current.selectedChanges.list, idx))
            )
            scrollRequest?.let(change::requestScroll)
        }
    }

    override fun showChange(changeIdx: Int, scrollRequest: DiffViewerScrollRequest?) {
        val current = _changesState.value?.result?.getOrNull() as? SimpleState ?: return
        if (changeIdx in current.selectedChanges.list.indices) {
            _changesState.value = ComputedResult.success(
                SimpleState(current.target, ListSelection.createAt(current.selectedChanges.list, changeIdx))
            )
            scrollRequest?.let(current.selectedChanges.list[changeIdx]::requestScroll)
        }
    }

    private class SimpleState(
        val target: GiteaPRDiffTarget,
        override val selectedChanges: ListSelection<GiteaPRDiffFileViewModel>,
    ) : CodeReviewDiffProcessorViewModel.State<GiteaPRDiffFileViewModel>
}
