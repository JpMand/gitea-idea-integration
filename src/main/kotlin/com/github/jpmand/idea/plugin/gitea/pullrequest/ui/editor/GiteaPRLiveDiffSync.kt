package com.github.jpmand.idea.plugin.gitea.pullrequest.ui.editor

import com.intellij.collaboration.async.launchNow
import com.intellij.collaboration.ui.codereview.editor.ReviewInEditorUtil
import com.intellij.diff.util.Range
import com.intellij.openapi.editor.Document
import kotlinx.coroutines.CoroutineScope

/**
 * Tracks how lines drift between a PR's head-SHA snapshot and a *live*, editable [Document] —
 * the seam that lets Phase 9's in-editor review annotations keep working as the user types,
 * unlike [GiteaPRDiffEditorModel]'s two fixed diff-viewer snapshots.
 *
 * Thin wrapper around the platform's own [ReviewInEditorUtil.trackDocumentDiffSync] (a `Nothing`-
 * returning suspend function that runs until cancelled, invoking its callback on every document
 * change) plus [ReviewInEditorUtil.transferLineToAfter]/[ReviewInEditorUtil.transferLineFromAfter]
 * for the actual line translation in each direction.
 */
class GiteaPRLiveDiffSync(cs: CoroutineScope, headContent: String, document: Document) {

    @Volatile
    private var ranges: List<Range> = emptyList()

    init {
        cs.launchNow {
            ReviewInEditorUtil.trackDocumentDiffSync(headContent, document) { ranges = it }
        }
    }

    /** Maps a 0-indexed line anchored to the PR's head-SHA snapshot to its current line in the
     * live document — always returns a best-effort line, even if the anchor line was since
     * deleted locally (snaps to the nearest surviving line), matching how existing-thread inlays
     * should keep showing somewhere sensible rather than disappearing on a local edit. */
    fun anchorToLive(anchorLineZeroIndexed: Int): Int = ReviewInEditorUtil.transferLineToAfter(ranges, anchorLineZeroIndexed)

    /** Maps a 0-indexed live-document line back to the PR's head-SHA snapshot — `null` when that
     * live line has no exact counterpart in the head snapshot (e.g. a line the user just typed),
     * which is deliberately treated as "not a safe place to anchor a new comment", not
     * approximated, since a wrong anchor would silently land a comment on the wrong server-side
     * line with no visible symptom in the editor. */
    fun liveToAnchor(liveLineZeroIndexed: Int): Int? = ReviewInEditorUtil.transferLineFromAfter(ranges, liveLineZeroIndexed)
}
