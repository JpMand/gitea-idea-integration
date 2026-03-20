package com.github.jpmand.idea.plugin.gitea.pullrequest.ui.list

import com.github.jpmand.idea.plugin.gitea.api.models.GiteaPullRequest
import com.intellij.ui.ColoredListCellRenderer
import com.intellij.ui.SimpleTextAttributes
import javax.swing.JList

/**
 * Cell renderer for a single pull-request row in [GiteaPRListComponent].
 * Renders: `#number title [state] by author (N 💬) [draft]`
 */
class GiteaPRListItemComponent : ColoredListCellRenderer<GiteaPullRequest>() {

    override fun customizeCellRenderer(
        list: JList<out GiteaPullRequest>,
        value: GiteaPullRequest?,
        index: Int,
        selected: Boolean,
        hasFocus: Boolean
    ) {
        value ?: return

        append("#${value.number} ", SimpleTextAttributes.GRAYED_ATTRIBUTES)
        append(value.title, SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES)

        val stateText = when {
            value.merged -> " [merged]"
            else -> " [${value.state ?: "open"}]"
        }
        append(stateText, SimpleTextAttributes.GRAY_ITALIC_ATTRIBUTES)
        append("  by ${value.author.login}", SimpleTextAttributes.GRAYED_ATTRIBUTES)

        if (value.reviewComments > 0) {
            append("  ${value.reviewComments} \uD83D\uDCAC", SimpleTextAttributes.GRAYED_ATTRIBUTES)
        }
        if (value.draft) {
            append("  [draft]", SimpleTextAttributes.GRAY_ITALIC_ATTRIBUTES)
        }

        // Show label chips
        value.labels.take(3).forEach { label ->
            append("  ${label.name}", SimpleTextAttributes.GRAY_ITALIC_ATTRIBUTES)
        }
    }
}
