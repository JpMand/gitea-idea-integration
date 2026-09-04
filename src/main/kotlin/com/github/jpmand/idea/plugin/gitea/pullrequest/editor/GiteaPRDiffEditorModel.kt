package com.github.jpmand.idea.plugin.gitea.pullrequest.editor

import com.github.jpmand.idea.plugin.gitea.pullrequest.review.GiteaPRDiscussionsViewModels
import com.intellij.collaboration.ui.codereview.diff.DiffLineLocation
import com.intellij.collaboration.ui.codereview.diff.DiscussionsViewOption
import com.intellij.collaboration.ui.codereview.editor.CodeReviewEditorGutterControlsModel
import com.intellij.collaboration.ui.codereview.editor.CodeReviewEditorModel
import com.intellij.diff.util.Side
import com.intellij.util.concurrency.annotations.RequiresEdt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted

/**
 * Per-editor view model that drives gutter controls (thread bubble icons) and manages
 * inlay panels (existing threads only — Milestone 1, read-only) in one diff editor.
 *
 * One instance is created per editor side (LEFT / RIGHT / null for unified) in [com.github.jpmand.idea.plugin.gitea.pullrequest.diff.GiteaPRDiffExtension].
 */
@Suppress("UnstableApiUsage")
class GiteaPRDiffEditorModel(
    cs: CoroutineScope,
    private val path: String,
    private val side: Side?,
    private val discussionsVm: GiteaPRDiscussionsViewModels,
    private val locationToLine: (DiffLineLocation) -> Int?,
    private val lineToLocation: (Int) -> DiffLineLocation?,
) : CodeReviewEditorModel<GiteaPRInlayModel> {

    // ── Thread inlays (existing server-side review threads) ────────────────

    private val threadInlays: StateFlow<List<GiteaPRInlayModel.Thread>> =
        combine(discussionsVm.threads, discussionsVm.discussionsViewOption) { result, viewOption ->
            if (viewOption == DiscussionsViewOption.DONT_SHOW) return@combine emptyList()
            val threadVms = result?.result?.getOrNull() ?: emptyList()
            threadVms.mapNotNull { vm ->
                if (vm.path != path) return@mapNotNull null
                if (viewOption == DiscussionsViewOption.UNRESOLVED_ONLY && vm.isResolved) return@mapNotNull null
                val lineIdx = when (side) {
                    Side.RIGHT -> vm.newLine?.let { locationToLine(Pair(Side.RIGHT, it - 1)) }
                    Side.LEFT -> vm.oldLine?.let { locationToLine(Pair(Side.LEFT, it - 1)) }
                    null -> vm.newLine?.let { locationToLine(Pair(Side.RIGHT, it - 1)) }
                        ?: vm.oldLine?.let { locationToLine(Pair(Side.LEFT, it - 1)) }
                } ?: return@mapNotNull null
                GiteaPRInlayModel.Thread(vm, lineIdx)
            }
        }.stateIn(cs, SharingStarted.Eagerly, emptyList())

    // ── Combined inlays ───────────────────────────────────────────────────

    override val inlays: StateFlow<Collection<GiteaPRInlayModel>> = threadInlays

    // ── Gutter controls state ─────────────────────────────────────────────
    // Read-only: no commentable-line ("+") affordance in Milestone 1.

    override val gutterControlsState: StateFlow<CodeReviewEditorGutterControlsModel.ControlsState?> =
        threadInlays.let { flow ->
            combine(flow, discussionsVm.discussionsViewOption) { threads, viewOption ->
                val linesWithComments = threads.mapNotNull { it.line.value }.toSet()
                object : CodeReviewEditorGutterControlsModel.ControlsState {
                    override val linesWithComments: Set<Int> = linesWithComments
                    override val linesWithNewComments: Set<Int> = emptySet()
                    override fun isLineCommentable(lineIdx: Int): Boolean = false
                }
            }
        }.stateIn(cs, SharingStarted.Eagerly, null)

    // ── Actions ───────────────────────────────────────────────────────────
    // Comment composition is Milestone 2 — no-ops here.

    @RequiresEdt
    override fun requestNewComment(lineIdx: Int) = Unit

    @RequiresEdt
    override fun cancelNewComment(lineIdx: Int) = Unit

    @RequiresEdt
    override fun toggleComments(lineIdx: Int) {
        // Milestone 1: all comments always visible; toggleComments is a no-op.
    }
}
