package com.github.jpmand.idea.plugin.gitea.pullrequest.editor

import com.github.jpmand.idea.plugin.gitea.pullrequest.review.GiteaPRDraftComment
import com.github.jpmand.idea.plugin.gitea.pullrequest.review.GiteaPRNewCommentViewModel
import com.github.jpmand.idea.plugin.gitea.pullrequest.review.GiteaPRThreadViewModel
import com.intellij.collaboration.ui.codereview.editor.CodeReviewInlayModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@Suppress("UnstableApiUsage")
sealed interface GiteaPRInlayModel : CodeReviewInlayModel {
    val editorLineIdx: Int

    /** Inlay for an existing server-side review thread. */
    class Thread(val vm: GiteaPRThreadViewModel, override val editorLineIdx: Int) : GiteaPRInlayModel {
        // Unique per emission — forces renderer refresh when threads are reloaded (avoids stale VMs).
        override val key: Any = Any()
        override val line: StateFlow<Int?> = MutableStateFlow(editorLineIdx)
        override val isVisible: StateFlow<Boolean> = MutableStateFlow(true)
    }

    /** Inlay for the compose panel when the user clicks the "+" gutter icon. */
    class NewComment(val vm: GiteaPRNewCommentViewModel, override val editorLineIdx: Int) : GiteaPRInlayModel {
        // Stable: object identity of the VM — same VM → same renderer (no flicker on unrelated updates).
        override val key: Any = vm
        override val line: StateFlow<Int?> = MutableStateFlow(editorLineIdx)
        override val isVisible: StateFlow<Boolean> = MutableStateFlow(true)
    }

    /** Inlay for a draft comment that has been "added to review" but not yet submitted to the server. */
    class DraftComment(
        val draft: GiteaPRDraftComment,
        override val editorLineIdx: Int,
        val onRemove: () -> Unit,
    ) : GiteaPRInlayModel {
        // Stable: localId is unique and consistent across `draftComments` emissions.
        override val key: Any = draft.localId
        override val line: StateFlow<Int?> = MutableStateFlow(editorLineIdx)
        override val isVisible: StateFlow<Boolean> = MutableStateFlow(true)
    }
}
