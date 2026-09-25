package com.github.jpmand.idea.plugin.gitea.pullrequest.ui.editor

import com.github.jpmand.idea.plugin.gitea.api.models.GiteaPRDraftComment
import com.github.jpmand.idea.plugin.gitea.api.rest.dto.CreatePullReviewOptions
import com.github.jpmand.idea.plugin.gitea.pullrequest.diff.GiteaPRChangedFile
import com.github.jpmand.idea.plugin.gitea.pullrequest.review.GiteaPRDiscussionsViewModels
import com.github.jpmand.idea.plugin.gitea.pullrequest.review.GiteaSuggestion
import com.github.jpmand.idea.plugin.gitea.pullrequest.review.GiteaSuggestionUtil
import com.github.jpmand.idea.plugin.gitea.pullrequest.ui.comment.GiteaPRSubmittableTextViewModel
import com.intellij.diff.util.Side
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Composer state for one not-yet-submitted new line comment — created by
 * [GiteaPRDiffEditorModel.requestNewComment] on a gutter "+" click, discarded (and its inlay
 * removed) on cancel via [onDismissed].
 *
 * "Submitting" here never hits the network: it finalizes into a local
 * [GiteaPRDraftComment][GiteaPRDiscussionsViewModels.addDraft] and this same inlay switches from
 * showing the composer to showing a compact, editable/removable draft row — see
 * [com.github.jpmand.idea.plugin.gitea.pullrequest.ui.editor.GiteaPRInlayComponentsFactory].
 */
class GiteaPRNewCommentEditorViewModel(
    project: Project,
    cs: CoroutineScope,
    file: GiteaPRChangedFile,
    val side: Side,
    /** 1-indexed file line, matching Gitea's `CreatePullReviewComment.newPosition`/`oldPosition`. */
    val line: Int,
    private val discussionsVm: GiteaPRDiscussionsViewModels,
    /** Set when this composer was opened from the suggested-change gutter bar — the caller wants
     * a read-only diff preview shown alongside the composer, and the encoded marker+fence block
     * appended to whatever the user types (or posted on its own if they type nothing — GitHub
     * itself allows a suggestion with no comment text), never exposed as raw editable text; see
     * [com.github.jpmand.idea.plugin.gitea.pullrequest.ui.editor.GiteaPRInlayComponentsFactory]. */
    val suggestion: GiteaSuggestion? = null,
    /** Set when this composer is being rehydrated from an already-persisted draft (e.g. on
     * reopening the diff/live editor) rather than freshly opened by the user — seeds [draft]
     * immediately so the inlay renders the compact draft row from the start instead of an empty
     * composer. */
    initialDraft: GiteaPRDraftComment? = null,
    /** Called once this composer's inlay should disappear entirely — either the user cancelled
     * before finalizing, or removed the draft after finalizing. */
    private val onDismissed: () -> Unit,
) {
    /** Renamed files: a comment on the base (old) side belongs to the old path. */
    val path: String = if (side == Side.LEFT) file.previousFilename ?: file.filename else file.filename

    private val _draft = MutableStateFlow(initialDraft)
    val draft: StateFlow<GiteaPRDraftComment?> = _draft.asStateFlow()

    /** Whether to offer "Send Single Comment Review" alongside "Start Review" — only when this
     * would be the *only* comment in the review (checked once, when the composer opens): sending a
     * single-comment review while other drafts already exist would silently bundle them all in,
     * contradicting the button's own "single comment" label. */
    val canSendAsSingleCommentReview: Boolean = discussionsVm.draftComments.value.isEmpty()

    val textVm = GiteaPRSubmittableTextViewModel(project, cs, requireNonBlank = suggestion == null) { body ->
        _draft.value = discussionsVm.addDraft(path, newLine, oldLine, fullBodyOf(body))
    }

    /** Cancels an in-progress (not yet finalized) composer. */
    fun cancel() = onDismissed()

    /**
     * Adds this comment as a draft, same as the primary "Start Review" submit, but immediately
     * follows it with a `COMMENT`-verdict review containing just that one comment — skipping the
     * usual "compose, then separately submit the review" two-step flow. [discussionsVm.submitReview]
     * manages its own busy-state/error notification and always submits whatever the *current* draft
     * batch is (fire-and-forget, not awaited) — [canSendAsSingleCommentReview] is what keeps that
     * batch to just this one comment in the common case.
     */
    fun submitAsSingleCommentReview() {
        val body = textVm.text.value
        if (suggestion == null && body.isBlank()) return
        // Shown as a draft row until the review goes through, so a failed submission leaves the
        // draft visible to retry or discard instead of only counted in the review toolbar.
        _draft.value = discussionsVm.addDraft(path, newLine, oldLine, fullBodyOf(body))
        discussionsVm.submitReview(CreatePullReviewOptions.Event.COMMENT, "", onSuccess = onDismissed)
        textVm.text.value = ""
    }

    private val newLine: Int? get() = if (side == Side.RIGHT) line else null
    private val oldLine: Int? get() = if (side == Side.LEFT) line else null

    private fun fullBodyOf(body: String): String = suggestion?.let {
        val encoded = GiteaSuggestionUtil.encode(it.oldStartLine, it.oldLines, it.newLines)
        if (body.isBlank()) encoded else "$body\n\n$encoded"
    } ?: body

    /** Reflects an edited draft's new body — called after [GiteaPRDiscussionsViewModels.updateDraft]
     * (the central store this inlay's own [draft] was seeded from, but never re-reads afterward)
     * succeeds, so the compact draft row this inlay renders shows the edit instead of reverting to
     * the pre-edit text on the next unrelated recomposition. */
    fun updateDraftBody(body: String) {
        _draft.value = _draft.value?.copy(body = body)
    }

    /** Removes an already-finalized draft — this inlay disappears with it. */
    fun removeDraft() {
        _draft.value?.let { discussionsVm.removeDraft(it.localId) }
        onDismissed()
    }
}
