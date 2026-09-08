package com.github.jpmand.idea.plugin.gitea.pullrequest.ui.details

import com.github.jpmand.idea.plugin.gitea.pullrequest.GiteaPullRequestsSettings
import com.intellij.collaboration.ui.codereview.details.model.CodeReviewChangeDetails
import com.intellij.collaboration.ui.codereview.details.model.CodeReviewChangeList
import com.intellij.collaboration.ui.codereview.details.model.CodeReviewChangeListViewModel
import com.intellij.collaboration.ui.codereview.details.model.CodeReviewChangeListViewModelBase
import com.intellij.collaboration.util.ChangesSelection
import com.intellij.collaboration.util.RefComparisonChange
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.util.concurrency.annotations.RequiresEdt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Backs the PR-details changes tree, mirroring GitLab's `GitLabMergeRequestChangeListViewModelImpl`:
 * a [CodeReviewChangeListViewModelBase] that also implements [CodeReviewChangeListViewModel.WithGrouping]
 * (directory tree, backed by [GiteaPullRequestsSettings.changesGroupingState]) and
 * [CodeReviewChangeListViewModel.WithViewedState] (the per-file "viewed" checkbox, persisted per PR
 * in [GiteaPullRequestsSettings.viewedPrFiles]).
 */
@Suppress("UnstableApiUsage")
class GiteaPRChangesTreeViewModel(
    parentCs: CoroutineScope,
    override val project: Project,
    private val prNumber: Int,
    changeList: CodeReviewChangeList,
    /** repo-relative path per change — the stable persistence key for viewed state. */
    private val relPathByChange: Map<RefComparisonChange, String>,
    private val onOpenChange: (Int) -> Unit,
) : CodeReviewChangeListViewModelBase(parentCs, changeList),
    CodeReviewChangeListViewModel.WithGrouping,
    CodeReviewChangeListViewModel.WithViewedState {

    private val settings: GiteaPullRequestsSettings get() = project.service()

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
        val idx = when (val selection = changesSelection.value) {
            is ChangesSelection.Precise -> selection.selectedIdx
            is ChangesSelection.Fuzzy -> selection.selectedIdx
            null -> -1
        }
        if (idx >= 0) onOpenChange(idx)
    }

    override fun showDiffPreview() = showDiff()
}
