package com.github.jpmand.idea.plugin.gitea.pullrequest.ui

import com.intellij.collaboration.messages.CollaborationToolsBundle
import com.intellij.collaboration.ui.codereview.timeline.thread.TimelineThreadCommentsPanel
import com.intellij.ui.CollectionListModel
import com.intellij.ui.components.ActionLink
import javax.swing.JComponent

/**
 * Thin wrapper around the platform's [TimelineThreadCommentsPanel] that corrects its displayed
 * fold count — everything else (fold/unfold, hover, animation) stays exactly the platform's own.
 *
 * [TimelineThreadCommentsPanel] hides every item but the first and last once there are more than
 * [TimelineThreadCommentsPanel.FOLD_THRESHOLD] (`3`) of them — i.e. it hides `size - 2` items —
 * but its own collapsed-count label is bound to `commentsModel.size - FOLD_THRESHOLD - 1`, which
 * is short by 2 for every foldable size (e.g. "0 more replies" for a 4-comment thread that
 * actually hides 2). That count model is a private `val` with no public setter, so the label can't
 * be corrected by feeding the panel different input — this patches the resulting `ActionLink`'s
 * text directly, once, right after construction.
 *
 * Safe only because comment lists never mutate in place here: threads are rebuilt wholesale on
 * every [com.github.jpmand.idea.plugin.gitea.pullrequest.review.GiteaPRDiscussionsViewModels.reload],
 * so the platform panel's own listener (which would stomp this back to the wrong value on an
 * in-place list change) never fires again after this runs.
 */
fun <T> createThreadCommentsPanel(comments: List<T>, commentComponentFactory: (T) -> JComponent): JComponent {
    val panel = TimelineThreadCommentsPanel(CollectionListModel(comments), commentComponentFactory)
    if (comments.size > TimelineThreadCommentsPanel.FOLD_THRESHOLD) {
        val hiddenCount = comments.size - 2
        panel.findActionLink()?.text = CollaborationToolsBundle.message("review.thread.more.replies", hiddenCount)
    }
    return panel
}

private fun JComponent.findActionLink(): ActionLink? {
    if (this is ActionLink) return this
    for (i in 0 until componentCount) {
        (getComponent(i) as? JComponent)?.findActionLink()?.let { return it }
    }
    return null
}
