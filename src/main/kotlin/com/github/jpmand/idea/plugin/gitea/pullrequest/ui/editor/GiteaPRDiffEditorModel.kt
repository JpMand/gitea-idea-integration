package com.github.jpmand.idea.plugin.gitea.pullrequest.ui.editor

import com.github.jpmand.idea.plugin.gitea.pullrequest.diff.GiteaPRChangedFile
import com.github.jpmand.idea.plugin.gitea.pullrequest.review.GiteaPRDiscussionsViewModels
import com.intellij.collaboration.ui.codereview.diff.DiffLineLocation
import com.intellij.collaboration.ui.codereview.diff.DiscussionsViewOption
import com.intellij.collaboration.ui.codereview.editor.CodeReviewEditorGutterControlsModel
import com.intellij.collaboration.ui.codereview.editor.CodeReviewEditorModel
import com.intellij.diff.util.Side
import com.intellij.openapi.project.Project
import com.intellij.util.concurrency.annotations.RequiresEdt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Per-editor view model that drives gutter controls (thread bubble icons, the "+" new-comment
 * affordance) and manages inlay panels (existing threads, plus new-comment composers/drafts) in
 * one diff editor.
 *
 * One instance is created per editor side (LEFT / RIGHT / null for unified) in [com.github.jpmand.idea.plugin.gitea.pullrequest.diff.GiteaPRDiffExtension].
 */
@Suppress("UnstableApiUsage")
class GiteaPRDiffEditorModel(
    private val cs: CoroutineScope,
    private val project: Project?,
    private val file: GiteaPRChangedFile,
    private val side: Side?,
    private val discussionsVm: GiteaPRDiscussionsViewModels,
    private val locationToLine: (DiffLineLocation) -> Int?,
    private val lineToLocation: (Int) -> DiffLineLocation?,
) : CodeReviewEditorModel<GiteaPRInlayModel> {

    private val path: String get() = file.filename

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

    // ── New-comment composer/draft inlays ───────────────────────────────────

    private val _newCommentVms = MutableStateFlow<Map<Int, GiteaPRNewCommentEditorViewModel>>(emptyMap())

    private val newCommentInlays: StateFlow<List<GiteaPRInlayModel.NewComment>> =
        _newCommentVms.map { byLine -> byLine.map { (lineIdx, vm) -> GiteaPRInlayModel.NewComment(vm, lineIdx) } }
            .stateIn(cs, SharingStarted.Eagerly, emptyList())

    // ── Combined inlays ──────────────────────────────────────────────────────

    override val inlays: StateFlow<Collection<GiteaPRInlayModel>> =
        combine(threadInlays, newCommentInlays) { threads, newComments -> threads + newComments }
            .stateIn(cs, SharingStarted.Eagerly, emptyList())

    // ── Gutter controls state ─────────────────────────────────────────────
    // linesWithComments (which lines get a highlighted gutter marker) is gated by the
    // "highlight lines with comments" setting. isLineCommentable is permissive — any line that
    // maps to a real file line on either side is commentable; whether Gitea's server itself
    // restricts to changed-only lines isn't determinable client-side (no unified-diff/hunk data
    // is fetched here — see GiteaPRDiffFileViewModel, which diffs full file content instead).

    override val gutterControlsState: StateFlow<CodeReviewEditorGutterControlsModel.ControlsState?> =
        combine(threadInlays, discussionsVm.discussionsViewOption, discussionsVm.highlightDiffLines, _newCommentVms) {
            threads, _, highlight, newComments ->
            val linesWithComments = if (highlight) threads.mapNotNull { it.line.value }.toSet() else emptySet()
            object : CodeReviewEditorGutterControlsModel.ControlsState {
                override val linesWithComments: Set<Int> = linesWithComments
                override val linesWithNewComments: Set<Int> = newComments.keys
                override fun isLineCommentable(lineIdx: Int): Boolean = lineToLocation(lineIdx) != null
            }
        }.stateIn(cs, SharingStarted.Eagerly, null)

    // ── Actions ───────────────────────────────────────────────────────────

    @RequiresEdt
    override fun requestNewComment(lineIdx: Int) {
        val proj = project ?: return
        if (_newCommentVms.value.containsKey(lineIdx)) return
        val (commentSide, zeroIndexedLine) = lineToLocation(lineIdx) ?: return
        val vm = GiteaPRNewCommentEditorViewModel(
            proj, cs, file, commentSide, zeroIndexedLine + 1, discussionsVm,
        ) { cancelNewComment(lineIdx) }
        _newCommentVms.value = _newCommentVms.value + (lineIdx to vm)
    }

    @RequiresEdt
    override fun cancelNewComment(lineIdx: Int) {
        _newCommentVms.value = _newCommentVms.value - lineIdx
    }

    @RequiresEdt
    override fun toggleComments(lineIdx: Int) {
        // All comments always visible; toggleComments is a no-op.
    }
}
