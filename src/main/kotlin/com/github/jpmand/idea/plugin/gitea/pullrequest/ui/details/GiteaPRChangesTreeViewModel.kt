package com.github.jpmand.idea.plugin.gitea.pullrequest.ui.details

import com.github.jpmand.idea.plugin.gitea.pullrequest.GiteaPullRequestsSettings
import com.intellij.collaboration.ui.codereview.details.model.CodeReviewChangeDetails
import com.intellij.collaboration.ui.codereview.details.model.CodeReviewChangeList
import com.intellij.collaboration.ui.codereview.details.model.CodeReviewChangeListViewModel
import com.intellij.collaboration.util.ChangesSelection
import com.intellij.collaboration.util.RefComparisonChange
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.platform.util.coroutines.childScope
import com.intellij.util.concurrency.annotations.RequiresEdt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Backs the PR-details changes tree. Implements the public [CodeReviewChangeListViewModel]
 * interfaces directly (no internal `CodeReviewChangeListViewModelBase`): it is a plain selection
 * state holder plus [CodeReviewChangeListViewModel.WithGrouping] (directory tree, backed by
 * [GiteaPullRequestsSettings.changesGroupingState]) and [CodeReviewChangeListViewModel.WithViewedState]
 * (the per-file "viewed" checkbox, persisted per PR in [GiteaPullRequestsSettings.viewedPrFiles]).
 */
@Suppress("UnstableApiUsage")
class GiteaPRChangesTreeViewModel(
    parentCs: CoroutineScope,
    override val project: Project,
    private val prNumber: Int,
    changeList: CodeReviewChangeList,
    /** repo-relative path per change — the stable persistence key for viewed state. */
    private val relPathByChange: Map<RefComparisonChange, String>,
    private val onOpenChange: (String) -> Unit,
) : CodeReviewChangeListViewModel.WithGrouping,
    CodeReviewChangeListViewModel.WithViewedState {

    private val cs = parentCs.childScope(javaClass.name)

    private val settings: GiteaPullRequestsSettings get() = project.service()

    override val changes: List<RefComparisonChange> = changeList.changes

    private val _changesSelection = MutableStateFlow<ChangesSelection>(ChangesSelection.Fuzzy(changes, -1))
    override val changesSelection: StateFlow<ChangesSelection> = _changesSelection.asStateFlow()

    private val _selectionRequests = MutableSharedFlow<CodeReviewChangeListViewModel.SelectionRequest>()
    override val selectionRequests: SharedFlow<CodeReviewChangeListViewModel.SelectionRequest> =
        _selectionRequests.asSharedFlow()

    override fun updateSelectedChanges(selection: ChangesSelection?) {
        if (selection != null) _changesSelection.value = selection
    }

    override val grouping: StateFlow<Set<String>> get() = settings.changesGroupingState

    override fun setGrouping(grouping: Collection<String>) {
        settings.changesGrouping = grouping.toSet()
    }

    override val detailsByChange: StateFlow<Map<RefComparisonChange, CodeReviewChangeDetails>> =
        settings.viewedFilesState(prNumber)
            .map { viewed -> changes.associateWith { CodeReviewChangeDetails(relPathByChange[it] in viewed, 0) } }
            .stateIn(
                cs,
                SharingStarted.Eagerly,
                changes.associateWith {
                    CodeReviewChangeDetails(settings.isViewed(prNumber, relPathByChange[it].orEmpty()), 0)
                },
            )

    @RequiresEdt
    override fun setViewedState(changes: Iterable<RefComparisonChange>, viewed: Boolean) {
        settings.setViewed(prNumber, changes.mapNotNull { relPathByChange[it] }, viewed)
    }

    override fun showDiff() {
        val selection = changesSelection.value
        val change = selection.changes.getOrNull(selection.selectedIdx) ?: return
        relPathByChange[change]?.let(onOpenChange)
    }

    override fun showDiffPreview() = showDiff()
}
