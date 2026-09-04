package com.github.jpmand.idea.plugin.gitea.pullrequest

import com.intellij.collaboration.async.mapState
import com.intellij.collaboration.ui.codereview.diff.DiscussionsViewOption
import com.intellij.collaboration.util.CollectableSerializablePersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.openapi.vcs.changes.ui.ChangesGroupingSupport
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable

@Service(Service.Level.PROJECT)
@State(
    name = "GiteaPullRequestsSettings",
    storages = [Storage(StoragePathMacros.WORKSPACE_FILE)],
    reportStatistic = false
)
@Suppress("UnstableApiUsage")
internal class GiteaPullRequestsSettings :
    CollectableSerializablePersistentStateComponent<GiteaPullRequestsSettings.State>(State()) {

    @Serializable
    data class State(
        val selectedUrlAndAccountId: Pair<String, String>? = null,
        val highlightDiffLinesInEditor: Boolean = false,
        val editorReviewEnabled: Boolean = true,
        val changesGrouping: Set<String> = setOf(
            ChangesGroupingSupport.DIRECTORY_GROUPING,
            ChangesGroupingSupport.MODULE_GROUPING
        ),
        val editorReviewViewOption: DiscussionsViewOption = DiscussionsViewOption.UNRESOLVED_ONLY
    )

    var selectedUrlAndAccountId: Pair<String, String>?
        get() = state.selectedUrlAndAccountId
        set(value) {
            updateStateAndEmit {
                it.copy(selectedUrlAndAccountId = value)
            }
        }

    var highlightDiffLinesInEditor: Boolean
        get() = state.highlightDiffLinesInEditor
        set(value) {
            updateStateAndEmit {
                it.copy(highlightDiffLinesInEditor = value)
            }
        }

    val highlightDiffLinesInEditorState: StateFlow<Boolean> = stateFlow.mapState { it.highlightDiffLinesInEditor }

    var editorReviewEnabled: Boolean
        get() = state.editorReviewEnabled
        set(value) {
            updateStateAndEmit {
                it.copy(editorReviewEnabled = value)
            }
        }

    var diffReviewViewOption: DiscussionsViewOption
        get() = state.editorReviewViewOption
        set(value) {
            updateStateAndEmit {
                it.copy(editorReviewViewOption = value)
            }
        }

    var changesGrouping: Set<String>
        get() = state.changesGrouping
        set(value) {
            updateStateAndEmit {
                it.copy(changesGrouping = value)
            }
        }
    val changesGroupingState: StateFlow<Set<String>> = stateFlow.mapState { it.changesGrouping }
}