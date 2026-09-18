package com.github.jpmand.idea.plugin.gitea.pullrequest.ui.editor

import com.github.jpmand.idea.plugin.gitea.pullrequest.diff.GiteaPRChangedFile
import com.github.jpmand.idea.plugin.gitea.pullrequest.review.GiteaPRDiscussionsViewModels
import com.github.jpmand.idea.plugin.gitea.pullrequest.review.GiteaSuggestion
import com.intellij.collaboration.ui.codereview.diff.DiffLineLocation
import com.intellij.collaboration.ui.codereview.diff.DiscussionsViewOption
import com.intellij.collaboration.ui.codereview.editor.CodeReviewEditorGutterControlsModel
import com.intellij.collaboration.ui.codereview.editor.CodeReviewEditorModel
import com.intellij.diff.util.Side
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.util.concurrency.annotations.RequiresEdt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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
    private val editor: Editor,
) : CodeReviewEditorModel<GiteaPRInlayModel> {

    private val path: String get() = file.filename

    // ── Thread inlays (existing server-side review threads) ────────────────

    /** Lines whose thread inlay is folded via the gutter comment icon (see [toggleComments]) —
     * the inlay hides, but the gutter icon itself stays visible as a reference (driven
     * unconditionally by [gutterControlsState], not this set). */
    private val _collapsedLines = MutableStateFlow<Set<Int>>(emptySet())

    private val threadInlays: StateFlow<List<GiteaPRInlayModel.Thread>> =
        combine(discussionsVm.threads, discussionsVm.discussionsViewOption, _collapsedLines) { result, viewOption, collapsed ->
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
                GiteaPRInlayModel.Thread(vm, lineIdx, editor, MutableStateFlow(lineIdx !in collapsed))
            }
        }.stateIn(cs, SharingStarted.Eagerly, emptyList())

    // ── New-comment composer/draft inlays ───────────────────────────────────

    private val _newCommentVms = MutableStateFlow<Map<Int, GiteaPRNewCommentEditorViewModel>>(emptyMap())

    init {
        // A finalized composer (draft.value != null) whose draft got dropped from the source of
        // truth — e.g. a bulk clear on review submit, see GiteaPRDiscussionsViewModels.submitReview
        // — must lose its inlay immediately, not linger until the diff is closed and reopened.
        cs.launch {
            discussionsVm.draftComments.collect { drafts ->
                val liveIds = drafts.map { it.localId }.toSet()
                _newCommentVms.value = _newCommentVms.value.filterValues { vm ->
                    val id = vm.draft.value?.localId
                    id == null || id in liveIds
                }
            }
        }
    }

    private val newCommentInlays: StateFlow<List<GiteaPRInlayModel.NewComment>> =
        _newCommentVms.map { byLine -> byLine.map { (lineIdx, vm) -> GiteaPRInlayModel.NewComment(vm, lineIdx) } }
            .stateIn(cs, SharingStarted.Eagerly, emptyList())

    // ── Combined inlays ──────────────────────────────────────────────────────

    override val inlays: StateFlow<Collection<GiteaPRInlayModel>> =
        combine(threadInlays, newCommentInlays) { threads, newComments -> threads + newComments }
            .stateIn(cs, SharingStarted.Eagerly, emptyList())

    // ── Gutter controls state ─────────────────────────────────────────────
    // linesWithComments (which lines get the gutter comment icon) is unconditional — every line
    // with a thread gets the icon, always, matching GitHub's behavior. isLineCommentable is
    // permissive — any line that maps to a real file line on either side is commentable; whether
    // Gitea's server itself restricts to changed-only lines isn't determinable client-side (no
    // unified-diff/hunk data is fetched here — see GiteaPRDiffFileViewModel, which diffs full file
    // content instead).

    override val gutterControlsState: StateFlow<CodeReviewEditorGutterControlsModel.ControlsState?> =
        combine(threadInlays, discussionsVm.discussionsViewOption, _newCommentVms) { threads, _, newComments ->
            val linesWithComments = threads.mapNotNull { it.line.value }.toSet()
            object : CodeReviewEditorGutterControlsModel.ControlsState {
                override val linesWithComments: Set<Int> = linesWithComments
                override val linesWithNewComments: Set<Int> = newComments.keys
                override fun isLineCommentable(lineIdx: Int): Boolean = lineToLocation(lineIdx) != null
            }
        }.stateIn(cs, SharingStarted.Eagerly, null)

    // ── Actions ───────────────────────────────────────────────────────────

    @RequiresEdt
    override fun requestNewComment(lineIdx: Int) {
        val (commentSide, zeroIndexedLine) = lineToLocation(lineIdx) ?: return
        createNewCommentVm(lineIdx, commentSide, zeroIndexedLine, suggestion = null)
    }

    /** Same as [requestNewComment], but attaches [suggestion] (rendered as a read-only diff
     * preview instead of raw editable text — see [GiteaPRNewCommentEditorViewModel]) and takes the
     * comment's anchor ([commentSide]/[zeroIndexedAnchorLine]) explicitly rather than deriving it
     * from [lineToLocation] — used by the suggested-change gutter bar, whose [displayLineIdx] is a
     * *live* line that's part of the local edit itself and therefore has no head-SHA counterpart
     * ([lineToLocation] would return `null` for it, same as [GiteaPRLiveDiffSync.liveToAnchor]'s
     * own doc comment explains). [suggestion]'s own `oldStartLine` already carries a real head-SHA
     * line, so the caller passes that straight through instead. Not part of
     * [CodeReviewEditorModel]'s fixed interface, so it's a plain sibling method rather than an
     * overload of [requestNewComment]. */
    @RequiresEdt
    fun requestSuggestion(displayLineIdx: Int, commentSide: Side, zeroIndexedAnchorLine: Int, suggestion: GiteaSuggestion) {
        createNewCommentVm(displayLineIdx, commentSide, zeroIndexedAnchorLine, suggestion)
    }

    private fun createNewCommentVm(displayLineIdx: Int, commentSide: Side, zeroIndexedLine: Int, suggestion: GiteaSuggestion?) {
        val proj = project ?: return
        if (_newCommentVms.value.containsKey(displayLineIdx)) return
        val vm = GiteaPRNewCommentEditorViewModel(
            proj, cs, file, commentSide, zeroIndexedLine + 1, discussionsVm, suggestion,
        ) { cancelNewComment(displayLineIdx) }
        _newCommentVms.value = _newCommentVms.value + (displayLineIdx to vm)
    }

    @RequiresEdt
    override fun cancelNewComment(lineIdx: Int) {
        _newCommentVms.value = _newCommentVms.value - lineIdx
    }

    /** Folds/unfolds the thread inlay at [lineIdx] — triggered by clicking the gutter comment
     * icon (see [gutterControlsState]'s `linesWithComments`, which stays unconditional so the
     * icon itself remains visible as a reference even while folded). */
    @RequiresEdt
    override fun toggleComments(lineIdx: Int) {
        _collapsedLines.value = if (lineIdx in _collapsedLines.value) {
            _collapsedLines.value - lineIdx
        } else {
            _collapsedLines.value + lineIdx
        }
    }
}
