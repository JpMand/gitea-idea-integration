package com.github.jpmand.idea.plugin.gitea.pullrequest.ui.editor

import com.github.jpmand.idea.plugin.gitea.pullrequest.review.GiteaPRThreadViewModel
import com.intellij.collaboration.ui.codereview.editor.CodeReviewInlayModel
import com.intellij.openapi.editor.Editor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Inlay kinds rendered in the diff editor: existing server-side review threads, and composers
 * for new (not-yet-submitted) line comments. */
@Suppress("UnstableApiUsage")
sealed interface GiteaPRInlayModel : CodeReviewInlayModel {
    val editorLineIdx: Int

    /** Inlay for an existing server-side review thread. */
    class Thread(
        val vm: GiteaPRThreadViewModel,
        override val editorLineIdx: Int,
        /** The diff-side editor this thread's inlay is shown in — needed to install the
         * hover-triggered anchor-line highlight (see [GiteaPRInlayComponentsFactory]). */
        val editor: Editor,
        /** Whether this thread's inlay is currently expanded — folded via the gutter comment
         * icon (see [GiteaPRDiffEditorModel.toggleComments]); the gutter icon itself stays shown
         * either way. */
        override val isVisible: StateFlow<Boolean>,
    ) : GiteaPRInlayModel {
        // Unique per emission — forces renderer refresh when threads are reloaded (avoids stale VMs).
        override val key: Any = Any()
        override val line: StateFlow<Int?> = MutableStateFlow(editorLineIdx)
    }

    /** Inlay for a new comment being composed (or already finalized into a local draft) at a
     * line that doesn't have an existing thread yet. */
    class NewComment(val vm: GiteaPRNewCommentEditorViewModel, override val editorLineIdx: Int) : GiteaPRInlayModel {
        override val key: Any = vm
        override val line: StateFlow<Int?> = MutableStateFlow(editorLineIdx)
        override val isVisible: StateFlow<Boolean> = MutableStateFlow(true)
    }
}
