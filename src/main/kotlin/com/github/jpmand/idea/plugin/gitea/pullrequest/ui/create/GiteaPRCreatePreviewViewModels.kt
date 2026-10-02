package com.github.jpmand.idea.plugin.gitea.pullrequest.ui.create

import com.github.jpmand.idea.plugin.gitea.pullrequest.GiteaPullRequestsSettings
import com.intellij.collaboration.ui.codereview.details.model.CodeReviewChangeDetails
import com.intellij.collaboration.ui.codereview.details.model.CodeReviewChangeListViewModel
import com.intellij.collaboration.ui.codereview.details.model.CodeReviewChangesViewModel
import com.intellij.collaboration.util.ChangesSelection
import com.intellij.collaboration.util.RefComparisonChange
import com.intellij.openapi.ListSelection
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vcs.changes.actions.diff.ShowDiffAction
import com.intellij.openapi.vcs.history.ShortVcsRevisionNumber
import com.intellij.openapi.vcs.history.VcsRevisionNumber
import com.intellij.vcs.log.VcsCommitMetadata
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.shareIn

/** The commit selector above the preview: "all commits" (-1) or one of [commits], newest first. */
@Suppress("UnstableApiUsage")
class GiteaPRCreateCommitsViewModel(cs: CoroutineScope, val commits: List<VcsCommitMetadata>) :
    CodeReviewChangesViewModel<VcsCommitMetadata> {

    private val _selectedIndex = MutableStateFlow(-1)

    override val reviewCommits: SharedFlow<List<VcsCommitMetadata>> = MutableStateFlow(commits).asStateFlow()
    override val selectedCommitIndex: SharedFlow<Int> = _selectedIndex.asStateFlow()
    override val selectedCommit: SharedFlow<VcsCommitMetadata?> =
        combine(reviewCommits, _selectedIndex) { list, idx -> list.getOrNull(idx) }.shareIn(cs, SharingStarted.Eagerly, replay = 1)

    override fun selectCommit(index: Int) {
        _selectedIndex.value = index.coerceIn(-1, commits.size - 1)
    }

    override fun selectNextCommit() {
        if (_selectedIndex.value < commits.size - 1) selectCommit(_selectedIndex.value + 1)
    }

    override fun selectPreviousCommit() {
        if (_selectedIndex.value > -1) selectCommit(_selectedIndex.value - 1)
    }

    override fun commitHash(commit: VcsCommitMetadata): String = commit.id.toShortString()
}

/**
 * The preview's changes tree, over local git [changes]: grouped like the Details tab's tree, and a
 * file opens in the IDE's own diff viewer (base vs head, read from git).
 */
@Suppress("UnstableApiUsage")
class GiteaPRCreateChangeListViewModel(
    override val project: Project,
    private val gitChanges: List<Change>,
) : CodeReviewChangeListViewModel.WithGrouping, CodeReviewChangeListViewModel.WithDetails {

    private val byRefChange: Map<RefComparisonChange, Change> = gitChanges.associateBy { it.toRefComparisonChange() }

    override val changes: List<RefComparisonChange> = byRefChange.keys.toList()

    private val _changesSelection = MutableStateFlow<ChangesSelection>(ChangesSelection.Fuzzy(changes, -1))
    override val changesSelection: StateFlow<ChangesSelection> = _changesSelection.asStateFlow()

    private val _selectionRequests = MutableSharedFlow<CodeReviewChangeListViewModel.SelectionRequest>()
    override val selectionRequests: SharedFlow<CodeReviewChangeListViewModel.SelectionRequest> = _selectionRequests.asSharedFlow()

    override fun updateSelectedChanges(selection: ChangesSelection?) {
        if (selection != null) _changesSelection.value = selection
    }

    private val settings: GiteaPullRequestsSettings get() = project.service()

    override val grouping: StateFlow<Set<String>> get() = settings.changesGroupingState

    override fun setGrouping(grouping: Collection<String>) {
        settings.changesGrouping = grouping.toSet()
    }

    // Nothing to count before the pull request exists.
    override val detailsByChange: StateFlow<Map<RefComparisonChange, CodeReviewChangeDetails>> =
        MutableStateFlow(changes.associateWith { CodeReviewChangeDetails(true, 0) }).asStateFlow()

    override fun showDiff() {
        val selection = _changesSelection.value
        val selected = selection.changes.getOrNull(selection.selectedIdx)?.let(byRefChange::get) ?: return
        ShowDiffAction.showDiffForChange(project, ListSelection.createAt(gitChanges, gitChanges.indexOf(selected)))
    }

    override fun showDiffPreview() = showDiff()

    private fun Change.toRefComparisonChange(): RefComparisonChange =
        RefComparisonChange(
            Revision(beforeRevision?.revisionNumber), beforeRevision?.file,
            Revision(afterRevision?.revisionNumber), afterRevision?.file,
        )

    private class Revision(private val revision: VcsRevisionNumber?) : ShortVcsRevisionNumber {
        override fun asString(): String = revision?.asString().orEmpty()
        override fun toShortString(): String = asString().take(8)
        override fun compareTo(other: VcsRevisionNumber?): Int = asString().compareTo(other?.asString().orEmpty())
        override fun equals(other: Any?): Boolean = other is Revision && other.asString() == asString()
        override fun hashCode(): Int = asString().hashCode()
    }
}
