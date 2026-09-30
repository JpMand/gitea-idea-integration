package com.github.jpmand.idea.plugin.gitea.pullrequest

import com.github.jpmand.idea.plugin.gitea.api.models.GiteaPRDraftComment
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
        val changesGrouping: Set<String> = setOf(
            ChangesGroupingSupport.DIRECTORY_GROUPING,
            ChangesGroupingSupport.MODULE_GROUPING
        ),
        val editorReviewViewOption: DiscussionsViewOption = DiscussionsViewOption.UNRESOLVED_ONLY,
        /** PR number -> not-yet-submitted inline review comments, so drafts survive closing and
         * reopening the diff/PR (see [GiteaPRDraftComment]). */
        val draftComments: Map<Int, List<GiteaPRDraftComment>> = emptyMap(),
    )

    var selectedUrlAndAccountId: Pair<String, String>?
        get() = state.selectedUrlAndAccountId
        set(value) {
            updateStateAndEmit {
                it.copy(selectedUrlAndAccountId = value)
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

    // ── Per-PR draft comments (persisted across sessions) ──────────────────

    fun draftComments(prNumber: Int): List<GiteaPRDraftComment> = state.draftComments[prNumber].orEmpty()

    /** [draftComments] as a flow, so every review surface of a PR shows the same drafts. */
    fun draftCommentsState(prNumber: Int): StateFlow<List<GiteaPRDraftComment>> =
        stateFlow.mapState { it.draftComments[prNumber].orEmpty() }

    /** Replaces the entire draft list for [prNumber] — callers own the merge logic (add/update/
     * remove/clear), this just writes the result through. */
    fun setDraftComments(prNumber: Int, drafts: List<GiteaPRDraftComment>) {
        updateStateAndEmit { st ->
            val next = if (drafts.isEmpty()) st.draftComments - prNumber else st.draftComments + (prNumber to drafts)
            st.copy(draftComments = next)
        }
    }
}