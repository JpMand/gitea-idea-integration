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
        val editorReviewViewOption: DiscussionsViewOption = DiscussionsViewOption.UNRESOLVED_ONLY,
        /** PR number -> repo-relative paths the user has marked "viewed" in the changes tree. */
        val viewedPrFiles: Map<Int, Set<String>> = emptyMap(),
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

    // ── Per-PR "viewed" file state (persisted across sessions) ────────────────

    fun viewedFilesState(prNumber: Int): StateFlow<Set<String>> =
        stateFlow.mapState { it.viewedPrFiles[prNumber].orEmpty() }

    fun isViewed(prNumber: Int, path: String): Boolean =
        state.viewedPrFiles[prNumber].orEmpty().contains(path)

    fun setViewed(prNumber: Int, paths: Collection<String>, viewed: Boolean) {
        if (paths.isEmpty()) return
        updateStateAndEmit { st ->
            val current = st.viewedPrFiles[prNumber].orEmpty()
            val next = if (viewed) current + paths else current - paths.toSet()
            st.copy(viewedPrFiles = st.viewedPrFiles + (prNumber to next))
        }
    }
}