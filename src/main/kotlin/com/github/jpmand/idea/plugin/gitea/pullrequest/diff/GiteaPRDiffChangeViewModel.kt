@file:Suppress("UnstableApiUsage")

package com.github.jpmand.idea.plugin.gitea.pullrequest.diff

import com.intellij.collaboration.async.computationStateFlow
import com.intellij.collaboration.async.withInitial
import com.intellij.collaboration.ui.codereview.diff.model.AsyncDiffViewModel
import com.intellij.collaboration.util.ComputedResult
import com.intellij.collaboration.util.RefComparisonChange
import com.intellij.diff.requests.DiffRequest
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.coroutineToIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vcs.changes.actions.diff.ChangeDiffRequestProducer
import git4idea.changes.createVcsChange
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.stateIn

/**
 * View model for a single changed file within a PR diff.
 * Implements [AsyncDiffViewModel] — the processor observes [request] and
 * renders the diff when it becomes available.
 *
 * Pattern mirrors `GitLabMergeRequestDiffChangeViewModelImpl`.
 */
class GiteaPRDiffChangeViewModel(
    private val cs: CoroutineScope,
    private val project: Project,
    val change: RefComparisonChange,
) : AsyncDiffViewModel {

    private val reloadRequests = Channel<Unit>(1, BufferOverflow.DROP_OLDEST)

    override val request: StateFlow<ComputedResult<DiffRequest>?> =
        computationStateFlow(reloadRequests.consumeAsFlow().withInitial(Unit)) {
            val vcsChange = change.createVcsChange(project)
            val producer = ChangeDiffRequestProducer.create(project, vcsChange)
                ?: error("Could not create diff request producer for ${change.filePath}")
            coroutineToIndicator {
                producer.process(
                    UserDataHolderBase(),
                    ProgressManager.getInstance().progressIndicator
                        ?: com.intellij.openapi.progress.EmptyProgressIndicator()
                )
            }.apply {
                putUserData(RefComparisonChange.KEY, change)
            }
        }.stateIn(cs, SharingStarted.Lazily, null)

    override fun reloadRequest() {
        reloadRequests.trySend(Unit)
    }

    override fun equals(other: Any?) =
        other is GiteaPRDiffChangeViewModel && change == other.change

    override fun hashCode(): Int = change.hashCode()
}
