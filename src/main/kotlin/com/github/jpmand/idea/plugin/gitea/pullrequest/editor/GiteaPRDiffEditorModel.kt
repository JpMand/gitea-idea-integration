package com.github.jpmand.idea.plugin.gitea.pullrequest.editor

import com.github.jpmand.idea.plugin.gitea.pullrequest.review.GiteaPRDiscussionsViewModels
import com.github.jpmand.idea.plugin.gitea.pullrequest.review.GiteaPRNewCommentViewModel
import com.intellij.collaboration.ui.codereview.diff.DiffLineLocation
import com.intellij.collaboration.ui.codereview.editor.CodeReviewEditorGutterControlsModel
import com.intellij.collaboration.ui.codereview.editor.CodeReviewEditorModel
import com.intellij.diff.util.Side
import com.intellij.util.concurrency.annotations.RequiresEdt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

/**
 * Per-editor view model that drives gutter controls (commentable-line icons, thread bubble icons)
 * and manages inlay panels (existing threads, compose drafts, committed drafts) in one diff editor.
 *
 * One instance is created per editor side (LEFT / RIGHT / null for unified) in [GiteaPRDiffExtension].
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
        discussionsVm.threads.map { result ->
            val threadVms = result?.result?.getOrNull() ?: emptyList()
            threadVms.mapNotNull { vm ->
                if (vm.path != path) return@mapNotNull null
                val lineIdx = when (side) {
                    Side.RIGHT -> vm.newLine?.let { locationToLine(Pair(Side.RIGHT, it - 1)) }
                    Side.LEFT -> vm.oldLine?.let { locationToLine(Pair(Side.LEFT, it - 1)) }
                    null -> vm.newLine?.let { locationToLine(Pair(Side.RIGHT, it - 1)) }
                        ?: vm.oldLine?.let { locationToLine(Pair(Side.LEFT, it - 1)) }
                } ?: return@mapNotNull null
                GiteaPRInlayModel.Thread(vm, lineIdx)
            }
        }.stateIn(cs, SharingStarted.Eagerly, emptyList())

    // ── Compose inlays (comment being typed — not yet added to review) ──────

    private val _pendingNewComments = MutableStateFlow<Map<Int, GiteaPRInlayModel.NewComment>>(emptyMap())

    // ── Draft inlays (added to review, not yet submitted to server) ─────────

    private val draftInlays: StateFlow<List<GiteaPRInlayModel.DraftComment>> =
        discussionsVm.draftComments.map { drafts ->
            drafts.filter { it.path == path }.mapNotNull { draft ->
                val lineIdx = when {
                    draft.newPosition != null -> locationToLine(Pair(Side.RIGHT, draft.newPosition - 1))
                    draft.oldPosition != null -> locationToLine(Pair(Side.LEFT, draft.oldPosition - 1))
                    else -> null
                } ?: return@mapNotNull null
                GiteaPRInlayModel.DraftComment(
                    draft = draft,
                    editorLineIdx = lineIdx,
                    onRemove = { discussionsVm.removeDraftComment(draft.localId) },
                )
            }
        }.stateIn(cs, SharingStarted.Eagerly, emptyList())

    // ── Combined inlays ───────────────────────────────────────────────────

    override val inlays: StateFlow<Collection<GiteaPRInlayModel>> =
        combine(threadInlays, _pendingNewComments, draftInlays) { threads, pending, drafts ->
            threads + pending.values + drafts
        }.stateIn(cs, SharingStarted.Eagerly, emptyList())

    // ── Gutter controls state ─────────────────────────────────────────────

    override val gutterControlsState: StateFlow<CodeReviewEditorGutterControlsModel.ControlsState?> =
        combine(threadInlays, _pendingNewComments, draftInlays) { threads, pending, drafts ->
            val linesWithComments = (
                    threads.mapNotNull { it.line.value } +
                    drafts.mapNotNull { it.line.value }
                    ).toMutableSet<Int>()
            val linesWithNewComments = pending.keys.toSet()
            object : CodeReviewEditorGutterControlsModel.ControlsState {
                override val linesWithComments: Set<Int> = linesWithComments
                override val linesWithNewComments: Set<Int> = linesWithNewComments
                override fun isLineCommentable(lineIdx: Int): Boolean = lineToLocation(lineIdx) != null
            }
        }.stateIn(cs, SharingStarted.Eagerly, null)

    // ── Actions ───────────────────────────────────────────────────────────

    @RequiresEdt
    override fun requestNewComment(lineIdx: Int) {
        if (_pendingNewComments.value.containsKey(lineIdx)) return
        val loc = lineToLocation(lineIdx) ?: return
        val (locSide, locLineIdx) = loc
        val newPosition = if (locSide == Side.RIGHT) locLineIdx + 1 else null
        val oldPosition = if (locSide == Side.LEFT) locLineIdx + 1 else null
        val vm = GiteaPRNewCommentViewModel(
            path = path,
            newPosition = newPosition,
            oldPosition = oldPosition,
            discussionsVm = discussionsVm,
            onCancel = { cancelNewComment(lineIdx) },
            onSubmit = { cancelNewComment(lineIdx) },
        )
        _pendingNewComments.update { it + (lineIdx to GiteaPRInlayModel.NewComment(vm, lineIdx)) }
    }

    @RequiresEdt
    override fun cancelNewComment(lineIdx: Int) {
        _pendingNewComments.update { it - lineIdx }
    }

    @RequiresEdt
    override fun toggleComments(lineIdx: Int) {
        // Phase 8: all comments always visible; toggleComments is a no-op.
    }
}
