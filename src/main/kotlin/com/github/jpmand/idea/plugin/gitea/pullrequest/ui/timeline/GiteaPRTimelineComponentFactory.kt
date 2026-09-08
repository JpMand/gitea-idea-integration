package com.github.jpmand.idea.plugin.gitea.pullrequest.ui.timeline

import com.github.jpmand.idea.plugin.gitea.api.models.GiteaUser
import com.github.jpmand.idea.plugin.gitea.pullrequest.ui.comment.GiteaPRCommentFieldFactory
import com.github.jpmand.idea.plugin.gitea.util.GiteaBundle
import com.intellij.collaboration.ui.HorizontalListPanel
import com.intellij.collaboration.ui.VerticalListPanel
import com.intellij.collaboration.ui.icon.IconsProvider
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.components.ActionLink
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.ScrollPaneConstants

/**
 * Assembles the PR activity-timeline editor, mirroring the bundled GitLab plugin's
 * `GitLabMergeRequestTimelineComponentFactory`: `[title, description, items, new-comment field]`
 * in a vertical column. The description is rendered through the same
 * [GiteaPRTimelineItemComponentFactory] shell as a synthetic comment. Read-only Milestone 1 —
 * the new-comment field's submit is a stub.
 */
@Suppress("UnstableApiUsage")
object GiteaPRTimelineComponentFactory {

    fun create(
        cs: CoroutineScope,
        vm: GiteaPRTimelineViewModel,
        itemFactory: GiteaPRTimelineItemComponentFactory,
        avatars: IconsProvider<GiteaUser>,
        onRefresh: () -> Unit,
    ): JComponent {
        val titleLabel = JBLabel("${vm.title} ${vm.number}").apply {
            font = JBFont.h2()
            border = JBUI.Borders.empty(4, 16, 8, 16)
        }

        val description = itemFactory.create(
            cs,
            GiteaPRTimelineItemViewModel.Comment(vm.author, vm.createdAt, vm.descriptionMarkdown, vm.pr.htmlUrl),
        )

        val itemsPanel = VerticalListPanel(0)

        cs.launch {
            vm.items.collect { computed ->
                itemsPanel.removeAll()
                val res = computed?.result
                when {
                    res == null -> itemsPanel.add(info(GiteaBundle.message("pull.request.timeline.loading")))
                    else -> res.fold(
                        onSuccess = { items -> items.forEach { itemsPanel.add(itemFactory.create(cs, it)) } },
                        onFailure = { itemsPanel.add(info(GiteaBundle.message("pull.request.timeline.error"))) },
                    )
                }
                itemsPanel.revalidate()
                itemsPanel.repaint()
            }
        }

        val commentField = JPanel(java.awt.BorderLayout()).apply {
            border = JBUI.Borders.empty(8, 16)
            add(GiteaPRCommentFieldFactory.create(cs, vm.newCommentVm, avatars, vm.author), java.awt.BorderLayout.CENTER)
            add(HorizontalListPanel(8).apply {
                add(ActionLink(GiteaBundle.message("pull.request.action.start.review")) {
                    com.github.jpmand.idea.plugin.gitea.pullrequest.ui.action.giteaWriteActionNotImplemented(
                        null, GiteaBundle.message("pull.request.action.start.review"),
                    )
                })
            }, java.awt.BorderLayout.SOUTH)
        }

        val column = VerticalListPanel(0).apply {
            add(titleLabel)
            add(description)
            add(itemsPanel)
            add(commentField)
        }

        val refreshBar = JPanel(java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 0, 0)).apply {
            isOpaque = false
            border = JBUI.Borders.empty(4, 12)
            add(ActionLink(GiteaBundle.message("pull.request.timeline.refresh")) { onRefresh() })
        }

        return JPanel(java.awt.BorderLayout()).apply {
            add(refreshBar, java.awt.BorderLayout.NORTH)
            add(
                ScrollPaneFactory.createScrollPane(column, true).apply {
                    horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
                },
                java.awt.BorderLayout.CENTER,
            )
        }
    }

    private fun info(text: String): JComponent =
        JBLabel(text).apply {
            foreground = UIUtil.getContextHelpForeground()
            border = JBUI.Borders.empty(12, 16)
        }
}
