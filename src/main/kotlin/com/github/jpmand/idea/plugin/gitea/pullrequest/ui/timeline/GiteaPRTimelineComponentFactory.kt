package com.github.jpmand.idea.plugin.gitea.pullrequest.ui.timeline

import com.github.jpmand.idea.plugin.gitea.api.models.GiteaUser
import com.github.jpmand.idea.plugin.gitea.pullrequest.ui.comment.GiteaPRCommentFieldFactory
import com.github.jpmand.idea.plugin.gitea.util.GiteaBundle
import com.intellij.collaboration.ui.VerticalListPanel
import com.intellij.collaboration.ui.icon.IconsProvider
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.components.ActionLink
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.panels.Wrapper
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.ScrollPaneConstants

/**
 * Assembles the PR activity-timeline editor, mirroring the bundled GitLab plugin's
 * `GitLabMergeRequestTimelineComponentFactory`: `[title, description, items, new-comment field]`
 * in a vertical column. The description is rendered through the same
 * [GiteaPRTimelineItemComponentFactory] shell as a synthetic comment.
 *
 * Reviews are read-only here (state chip, body, threads — see
 * [GiteaPRTimelineItemComponentFactory.review]): review *authoring* lives in the future
 * Diff/Review-mode live editor, not this Timeline.
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
            // id = 0 is never a real comment id (Gitea's start at 1) — the description isn't
            // editable/deletable through the comment-edit path (out of scope; see plan notes).
            GiteaPRTimelineItemViewModel.Comment(0L, vm.author, vm.createdAt, vm.descriptionMarkdown, vm.pr.htmlUrl),
        )

        val itemsPanel = VerticalListPanel(0)
        // Reused across emissions so an unrelated in-place update (see the "In-place updates"
        // section of GiteaPRTimelineViewModel) doesn't rebuild every item's component from
        // scratch — that would destroy, e.g., a reply composer's in-progress draft text on a
        // thread nobody touched. Each in-place update returns items.map { ... else item }, so an
        // untouched item is the exact same (`==`) value as its previous emission and its
        // already-built component is reused as-is; only items whose value actually changed get a
        // new component, and the panel's children are patched in place rather than torn down.
        val itemComponents = mutableMapOf<GiteaPRTimelineItemViewModel, JComponent>()

        fun renderItems(items: List<GiteaPRTimelineItemViewModel>) {
            itemComponents.keys.retainAll(items.toSet())
            items.forEachIndexed { index, item ->
                val component = itemComponents.getOrPut(item) { itemFactory.create(cs, item) }
                if (index >= itemsPanel.componentCount || itemsPanel.getComponent(index) !== component) {
                    if (index < itemsPanel.componentCount) itemsPanel.remove(index)
                    itemsPanel.add(component, index)
                }
            }
            while (itemsPanel.componentCount > items.size) {
                itemsPanel.remove(itemsPanel.componentCount - 1)
            }
        }

        cs.launch {
            vm.items.collect { computed ->
                val res = computed?.result
                when {
                    res == null -> {
                        itemComponents.clear()
                        itemsPanel.removeAll()
                        itemsPanel.add(info(GiteaBundle.message("pull.request.timeline.loading")))
                    }
                    else -> res.fold(
                        onSuccess = { items -> renderItems(items) },
                        onFailure = {
                            itemComponents.clear()
                            itemsPanel.removeAll()
                            itemsPanel.add(info(GiteaBundle.message("pull.request.timeline.error")))
                        },
                    )
                }
                itemsPanel.revalidate()
                itemsPanel.repaint()
            }
        }

        val commentField = JPanel(java.awt.BorderLayout()).apply {
            border = JBUI.Borders.empty(8, 16)
            add(commentFieldPanel(cs, vm, avatars), java.awt.BorderLayout.CENTER)
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

    /**
     * The "leave a comment" field needs the signed-in account's [GiteaUser] (for its avatar
     * icon), which is loaded asynchronously ([GiteaPRTimelineViewModel.currentUser]) rather than
     * available up front like [GiteaPRTimelineViewModel.author] — swap it in reactively via a
     * [Wrapper] once it arrives instead of blocking panel construction on it.
     */
    private fun commentFieldPanel(cs: CoroutineScope, vm: GiteaPRTimelineViewModel, avatars: IconsProvider<GiteaUser>): JComponent {
        val wrapper = Wrapper()
        cs.launch {
            vm.currentUser.collect { user ->
                wrapper.setContent(
                    user?.let { GiteaPRCommentFieldFactory.create(cs, vm.newCommentVm, avatars, it, vm.mentionCandidates) },
                )
                wrapper.revalidate()
                wrapper.repaint()
            }
        }
        return wrapper
    }

    private fun info(text: String): JComponent =
        JBLabel(text).apply {
            foreground = UIUtil.getContextHelpForeground()
            border = JBUI.Borders.empty(12, 16)
        }
}
