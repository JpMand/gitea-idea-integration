package com.github.jpmand.idea.plugin.gitea.pullrequest.ui.list

import com.github.jpmand.idea.plugin.gitea.api.models.GiteaPullRequest
import com.github.jpmand.idea.plugin.gitea.pullrequest.ui.filters.GiteaPRListSearchPanelFactory
import com.github.jpmand.idea.plugin.gitea.util.GiteaBundle
import com.intellij.collaboration.ui.codereview.list.NamedCollection
import com.intellij.collaboration.ui.codereview.list.ReviewListComponentFactory
import com.intellij.collaboration.ui.codereview.list.ReviewListItemPresentation
import com.intellij.collaboration.ui.codereview.list.ReviewListUtil
import com.intellij.collaboration.ui.codereview.list.TagPresentation
import com.intellij.collaboration.ui.codereview.list.UserPresentation
import com.intellij.ui.ColorHexUtil
import icons.CollaborationToolsIcons
import kotlinx.coroutines.CoroutineScope
import java.awt.BorderLayout
import java.awt.Color
import javax.swing.JComponent
import javax.swing.JPanel

@Suppress("UnstableApiUsage")
class GiteaPRListPanel(
    private val cs: CoroutineScope,
    private val vm: GiteaPRListViewModel,
    private val onPRSelected: ((GiteaPullRequest) -> Unit)? = null,
) {

    private var list: javax.swing.JList<GiteaPullRequest>? = null

    fun clearSelection() {
        list?.clearSelection()
    }

    fun create(): JComponent {
        val l = ReviewListComponentFactory(vm.listModel).create { pr ->
            createPresentation(pr)
        }
        list = l

        l.addListSelectionListener { e ->
            if (!e.valueIsAdjusting) {
                l.selectedValue?.let { pr -> onPRSelected?.invoke(pr) }
            }
        }

        val searchPanel = GiteaPRListSearchPanelFactory(vm.searchVm).create(cs)
        val scrollPane = ReviewListUtil.wrapWithLazyVerticalScroll(cs, l) { /* pagination deferred */ }

        return JPanel(BorderLayout()).apply {
            add(searchPanel, BorderLayout.NORTH)
            add(scrollPane, BorderLayout.CENTER)
        }
    }

    private fun createPresentation(pr: GiteaPullRequest): ReviewListItemPresentation {
        val stateText: String? = when {
            pr.draft -> GiteaBundle.message("pull.request.state.draft")
            pr.state == "closed" -> GiteaBundle.message("pull.request.state.closed")
            else -> null
        }

        val author = UserPresentation.Simple(
            username = pr.author.login,
            fullName = pr.author.fullName,
            avatarIcon = CollaborationToolsIcons.Review.DefaultAvatar,
        )

        val tags = pr.labels.map { label ->
            TagPresentation.Simple(label.name, parseColor(label.color))
        }

        val assignees = pr.assignees.map { user ->
            UserPresentation.Simple(user.login, user.fullName, CollaborationToolsIcons.Review.DefaultAvatar)
        }

        val commentsCounter = if (pr.reviewComments > 0) {
            ReviewListItemPresentation.CommentsCounter(
                pr.reviewComments,
                GiteaBundle.message("pull.request.comments.tooltip", pr.reviewComments),
            )
        } else null

        return ReviewListItemPresentation.Simple(
            title = pr.title,
            id = "#${pr.number}",
            createdDate = pr.createdAt,
            author = author,
            tagGroup = NamedCollection.create(
                GiteaBundle.message("pull.request.labels.popup", tags.size), tags
            ),
            state = stateText,
            userGroup1 = NamedCollection.create(
                GiteaBundle.message("pull.request.assignees.popup", assignees.size), assignees
            ),
            commentsCounter = commentsCounter,
        )
    }

    private fun parseColor(hex: String): Color? = try {
        ColorHexUtil.fromHex(hex.trimStart('#'))
    } catch (_: Exception) {
        null
    }
}
