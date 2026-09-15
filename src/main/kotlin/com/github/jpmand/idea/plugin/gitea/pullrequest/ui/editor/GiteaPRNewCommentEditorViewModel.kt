package com.github.jpmand.idea.plugin.gitea.pullrequest.ui.editor

import com.github.jpmand.idea.plugin.gitea.api.models.GiteaPRDraftComment
import com.github.jpmand.idea.plugin.gitea.pullrequest.diff.GiteaPRChangedFile
import com.github.jpmand.idea.plugin.gitea.pullrequest.review.GiteaPRDiscussionsViewModels
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
    /** Called once this composer's inlay should disappear entirely — either the user cancelled
     * before finalizing, or removed the draft after finalizing. */
    private val onDismissed: () -> Unit,
) {
    /** Renamed files: a comment on the base (old) side belongs to the old path. */
    val path: String = if (side == Side.LEFT) file.previousFilename ?: file.filename else file.filename

    private val _draft = MutableStateFlow<GiteaPRDraftComment?>(null)
    val draft: StateFlow<GiteaPRDraftComment?> = _draft.asStateFlow()

    val textVm = GiteaPRSubmittableTextViewModel(project, cs) { body ->
        val newLine = if (side == Side.RIGHT) line else null
        val oldLine = if (side == Side.LEFT) line else null
        _draft.value = discussionsVm.addDraft(path, newLine, oldLine, body)
    }

    /** Cancels an in-progress (not yet finalized) composer. */
    fun cancel() = onDismissed()

    /** Removes an already-finalized draft — this inlay disappears with it. */
    fun removeDraft() {
        _draft.value?.let { discussionsVm.removeDraft(it.localId) }
        onDismissed()
    }
}
