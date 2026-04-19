package com.github.jpmand.idea.plugin.gitea.pullrequest.review

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ViewModel for a comment currently being composed in the diff gutter.
 *
 * Lifecycle: created when the user opens the compose inlay on a diff line;
 * destroyed when the user submits ([submit]) or cancels ([cancel]).
 *
 * Submitting adds the draft to [GiteaPRDiscussionsViewModels.draftComments]; the draft
 * is sent to the server as part of the batch review in Phase 9.
 *
 * @param path File path the comment is anchored to.
 * @param newPosition 1-indexed head-file line; null for base-only (deleted-line) comments.
 * @param oldPosition 1-indexed base-file line; null for head-only (added-line) comments.
 * @param onCancel Invoked when the user cancels the compose inlay (e.g., to remove the UI).
 * @param onSubmit Invoked after the draft has been added (e.g., to replace the compose inlay with a draft inlay).
 */
class GiteaPRNewCommentViewModel(
    val path: String,
    val newPosition: Int?,
    val oldPosition: Int?,
    private val discussionsVm: GiteaPRDiscussionsViewModels,
    private val onCancel: () -> Unit,
    private val onSubmit: () -> Unit,
) {
    private val _text = MutableStateFlow("")
    val text: StateFlow<String> = _text.asStateFlow()

    fun updateText(value: String) {
        _text.value = value
    }

    val canSubmit: Boolean get() = _text.value.isNotBlank()

    /**
     * Adds the current text as a draft comment to [GiteaPRDiscussionsViewModels].
     *
     * @return The [GiteaPRDraftComment.localId] assigned to the new draft.
     * @throws IllegalStateException if [text] is blank.
     */
    fun submit(): Long {
        val body = _text.value.trim()
        check(body.isNotEmpty()) { "Cannot submit a blank comment" }
        val id = discussionsVm.addDraftComment(path, newPosition, oldPosition, body)
        onSubmit()
        return id
    }

    /** Cancels the in-progress comment and invokes the [onCancel] callback. */
    fun cancel() = onCancel()
}
