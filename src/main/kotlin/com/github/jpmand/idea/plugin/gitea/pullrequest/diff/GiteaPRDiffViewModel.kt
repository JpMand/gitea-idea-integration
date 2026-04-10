@file:Suppress("UnstableApiUsage")

package com.github.jpmand.idea.plugin.gitea.pullrequest.diff

import com.github.jpmand.idea.plugin.gitea.api.rest.models.pr.GiteaPRFileStatusEnum
import com.github.jpmand.idea.plugin.gitea.api.rest.repoListPullRequestFiles
import com.github.jpmand.idea.plugin.gitea.pullrequest.service.GiteaPullRequestsProjectService
import com.intellij.collaboration.util.ComputedResult
import com.intellij.collaboration.util.RefComparisonChange
import com.intellij.collaboration.util.filePath
import com.intellij.collaboration.util.fileStatus
import com.intellij.openapi.ListSelection
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vcs.FilePath
import com.intellij.openapi.vcs.VcsUtil
import com.intellij.openapi.vcs.changes.ui.PresentableChange
import com.intellij.collaboration.ui.codereview.diff.model.AsyncDiffViewModel
import com.intellij.collaboration.ui.codereview.diff.model.CodeReviewDiffProcessorViewModel
import com.intellij.collaboration.ui.codereview.diff.model.DiffViewerScrollRequest
import git4idea.history.GitRevisionNumber
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * View model for the PR diff viewer.
 * Implements [CodeReviewDiffProcessorViewModel] over [GiteaPRDiffChangeViewModel]
 * so that [com.intellij.collaboration.ui.codereview.diff.AsyncDiffRequestProcessorFactory.createIn]
 * can drive the diff UI.
 *
 * One instance is cached per PR number in [GiteaPRDiffService].
 */
class GiteaPRDiffViewModel(
    private val cs: CoroutineScope,
    private val project: Project,
    private val prService: GiteaPullRequestsProjectService,
    val owner: String,
    val repo: String,
    val prNumber: Int,
    val baseSha: String,
    val headSha: String,
) : CodeReviewDiffProcessorViewModel<GiteaPRDiffChangeViewModel> {

    private val _changesState =
        MutableStateFlow<ComputedResult<CodeReviewDiffProcessorViewModel.State<GiteaPRDiffChangeViewModel>>?>(null)
    override val changes: StateFlow<ComputedResult<CodeReviewDiffProcessorViewModel.State<GiteaPRDiffChangeViewModel>>?> =
        _changesState

    private var loadedChanges: List<GiteaPRDiffChangeViewModel> = emptyList()

    /** If set before loading completes, this relative path will be pre-selected. */
    @Volatile
    private var pendingRelativePath: String? = null

    init {
        cs.launch {
            _changesState.value = ComputedResult.loading()
            runCatching { loadChanges() }
                .onSuccess { vms ->
                    loadedChanges = vms
                    val startIdx = pendingRelativePath
                        ?.let { path -> vms.indexOfFirst { it.change.filePath.path.endsWith(path) }.takeIf { it >= 0 } }
                        ?: 0
                    _changesState.value = ComputedResult.success(
                        StateImpl(ListSelection.createAt(vms, startIdx))
                    )
                }
                .onFailure { e ->
                    _changesState.value = ComputedResult.failure(e)
                }
        }
    }

    private suspend fun loadChanges(): List<GiteaPRDiffChangeViewModel> {
        val api = prService.getOrLoadApiForActiveRepo() ?: error("No API available for PR diff")
        val repoRoot = prService.activeRepoMappingState.value
            ?.gitRepository?.root?.path
            ?: error("No git repository root found")
        val files = api.repoListPullRequestFiles(owner, repo, prNumber)
        return files.map { file ->
            val absPath = "$repoRoot/${file.filename}"
            val filePathAfter: FilePath? =
                if (file.status == GiteaPRFileStatusEnum.deleted) null
                else VcsUtil.getFilePath(absPath, false)
            val filePathBefore: FilePath? =
                if (file.status == GiteaPRFileStatusEnum.added) null
                else VcsUtil.getFilePath(absPath, false)
            val change = RefComparisonChange(
                revisionNumberBefore = GitRevisionNumber(baseSha),
                filePathBefore = filePathBefore,
                revisionNumberAfter = GitRevisionNumber(headSha),
                filePathAfter = filePathAfter,
            )
            GiteaPRDiffChangeViewModel(cs, project, change)
        }
    }

    override fun showChange(change: GiteaPRDiffChangeViewModel, scrollRequest: DiffViewerScrollRequest?) {
        val current = loadedChanges
        val idx = current.indexOf(change)
        if (idx >= 0) {
            _changesState.value = ComputedResult.success(StateImpl(ListSelection.createAt(current, idx)))
        }
    }

    override fun showChange(changeIdx: Int, scrollRequest: DiffViewerScrollRequest?) {
        val current = loadedChanges
        if (changeIdx in current.indices) {
            _changesState.value = ComputedResult.success(StateImpl(ListSelection.createAt(current, changeIdx)))
        }
    }

    /**
     * Selects the change whose [RefComparisonChange.filePath] ends with [relativePath].
     * If loading is still in progress, the selection is deferred until loading completes.
     */
    fun showChangeForPath(relativePath: String) {
        val current = loadedChanges
        if (current.isNotEmpty()) {
            val vm = current.firstOrNull { it.change.filePath.path.endsWith(relativePath) }
            if (vm != null) showChange(vm)
        } else {
            pendingRelativePath = relativePath
        }
    }

    private class StateImpl(
        override val selectedChanges: ListSelection<GiteaPRDiffChangeViewModel>,
    ) : CodeReviewDiffProcessorViewModel.State<GiteaPRDiffChangeViewModel>

    companion object {
        val KEY: Key<GiteaPRDiffViewModel> = Key.create("Gitea.PR.Diff.ViewModel")
    }
}

/** Convenience [PresentableChange] backed by a [RefComparisonChange]. */
internal fun RefComparisonChange.asPresentableChange(): PresentableChange = object : PresentableChange {
    override fun getFilePath(): FilePath = this@asPresentableChange.filePath
    override fun getFileStatus() = this@asPresentableChange.fileStatus
}
