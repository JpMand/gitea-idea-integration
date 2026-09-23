package com.github.jpmand.idea.plugin.gitea.pullrequest.ui.details

import com.github.jpmand.idea.plugin.gitea.pullrequest.GiteaPullRequestsSettings
import com.github.jpmand.idea.plugin.gitea.pullrequest.review.GiteaPRDiscussionsViewModels
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * Backs the PR-details changes tree. Implements the public [CodeReviewChangeListViewModel]
 * interfaces directly (no internal `CodeReviewChangeListViewModelBase`): it is a plain selection
 * state holder plus [CodeReviewChangeListViewModel.WithGrouping] (directory tree, backed by
 * [GiteaPullRequestsSettings.changesGroupingState]) and [CodeReviewChangeListViewModel.WithViewedState]
 * (the per-file "viewed" checkbox, persisted per PR in [GiteaPullRequestsSettings.viewedPrFiles],
 * plus the per-file review-comment count badge, derived from [discussionsVm]).
 */
@Suppress("UnstableApiUsage")
class GiteaPRChangesTreeViewModel(
    parentCs: CoroutineScope,
    override val project: Project,
    private val prNumber: Int,
    changeList: CodeReviewChangeList,
    /** repo-relative path per change — the stable persistence key for viewed state. */
    private val relPathByChange: Map<RefComparisonChange, String>,
    /** the pre-rename path per change, present only for renamed/copied files — a comment can be
     * anchored to either side of a rename (see [com.github.jpmand.idea.plugin.gitea.pullrequest.ui.editor.GiteaPRNewCommentEditorViewModel.path]),
     * so the badge count must sum both paths for those files. */
    private val previousRelPathByChange: Map<RefComparisonChange, String>,
    private val discussionsVm: GiteaPRDiscussionsViewModels,
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
        combine(settings.viewedFilesState(prNumber), discussionsVm.threads) { viewed, threadsResult ->
            val countsByPath = threadsResult?.result?.getOrNull().orEmpty()
                .filter { it.path != null }
                .groupBy { it.path!! }
                .mapValues { (_, threads) -> threads.sumOf { it.commentVMs.size } }
            changes.associateWith { change ->
                val count = (relPathByChange[change]?.let { countsByPath[it] } ?: 0) +
                    (previousRelPathByChange[change]?.let { countsByPath[it] } ?: 0)
                CodeReviewChangeDetails(relPathByChange[change] in viewed, count)
            }
        }.stateIn(
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
