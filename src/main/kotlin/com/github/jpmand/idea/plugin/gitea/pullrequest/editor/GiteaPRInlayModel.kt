package com.github.jpmand.idea.plugin.gitea.pullrequest.editor

import com.github.jpmand.idea.plugin.gitea.pullrequest.review.GiteaPRThreadViewModel
import com.intellij.collaboration.ui.codereview.editor.CodeReviewInlayModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Milestone-1 (read-only) inlay model — only existing server-side review threads are shown.
 * Comment composition (NewComment/DraftComment) is Milestone 2.
 */
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
}
