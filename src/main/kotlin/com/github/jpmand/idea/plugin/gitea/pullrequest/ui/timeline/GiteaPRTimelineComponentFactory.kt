package com.github.jpmand.idea.plugin.gitea.pullrequest.ui.timeline

import com.github.jpmand.idea.plugin.gitea.api.models.GiteaUser
import com.github.jpmand.idea.plugin.gitea.pullrequest.ui.comment.GiteaPRCommentFieldFactory
import com.github.jpmand.idea.plugin.gitea.util.GiteaBundle
import com.intellij.collaboration.ui.LoadingLabel
import com.intellij.collaboration.ui.SimpleHtmlPane
import com.intellij.collaboration.ui.VerticalListPanel
import com.intellij.collaboration.ui.codereview.CodeReviewChatItemUIUtil.ComponentType
import com.intellij.collaboration.ui.codereview.CodeReviewTimelineUIUtil
import com.intellij.collaboration.ui.codereview.CodeReviewTitleUIUtil
import com.intellij.collaboration.ui.codereview.list.error.ErrorStatusPanelFactory
import com.intellij.collaboration.ui.codereview.list.error.ErrorStatusPresenter
import com.intellij.collaboration.ui.setHtmlBody
import com.intellij.collaboration.ui.util.swingAction
import com.intellij.collaboration.ui.icon.IconsProvider
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.components.ActionLink
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.panels.Wrapper
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.border.EmptyBorder
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
        // The platform's review title: bold, with a grey "#N" that links to the PR, as in the GitHub plugin.
        val titleLabel = SimpleHtmlPane().apply {
            font = JBFont.h2().asBold()
            border = JBUI.Borders.empty(CodeReviewTimelineUIUtil.HEADER_VERT_PADDING, CodeReviewTimelineUIUtil.ITEM_HOR_PADDING)
        }

        cs.launch {
            vm.pr.map { CodeReviewTitleUIUtil.createTitleText(it.title, vm.number, it.htmlUrl, GiteaBundle.message("pull.request.details.title.tooltip")) }
                .distinctUntilChanged().collect { titleLabel.setHtmlBody(it) }
        }

        val description = Wrapper()
        cs.launch {
            vm.pr.map { pr ->
                // id = 0 is never a real comment id (Gitea's start at 1) — the description isn't
                // editable/deletable through the comment-edit path (out of scope; see plan notes).
                GiteaPRTimelineItemViewModel.Comment(0L, pr.author, pr.createdAt, pr.body?.takeIf { it.isNotBlank() }, pr.htmlUrl)
            }.distinctUntilChanged().collectLatest { item ->
                // Scoped to this version of the description, so replacing it cancels the old one's work.
                coroutineScope {
                    description.setContent(itemFactory.createDescription(this, item))
                    description.revalidate()
                    description.repaint()
                    awaitCancellation()
                }
            }
        }

        val itemsPanel = VerticalListPanel(0)
        // Reused across emissions so an unrelated in-place update (see the "In-place updates"
        // section of GiteaPRTimelineViewModel) doesn't rebuild every item's component from
        // scratch — that would destroy, e.g., a reply composer's in-progress draft text on a
        // thread nobody touched. Each in-place update returns items.map { ... else item }, so an
        // untouched item is the exact same (`==`) value as its previous emission and its
        // already-built component is reused as-is; only items whose value actually changed get a
        // new component, and the panel's children are patched in place rather than torn down.
        //
        // Each item gets its own scope, cancelled when its component is dropped: an item's
        // collectors and its review threads' diff editors would otherwise live as long as the tab.
        var itemComponents = mutableMapOf<GiteaPRTimelineItemViewModel, BuiltItem>()
        // Set by Refresh: the next render builds every item anew.
        var rebuildAll = false

        fun dropItems() {
            itemComponents.values.forEach { it.job.cancel() }
            itemComponents = mutableMapOf()
        }

        fun buildItem(item: GiteaPRTimelineItemViewModel): BuiltItem {
            val job = SupervisorJob(cs.coroutineContext[Job])
            return BuiltItem(itemFactory.create(CoroutineScope(cs.coroutineContext + job), item), job)
        }

        fun renderItems(items: List<GiteaPRTimelineItemViewModel>) {
            val previous = itemComponents
            val reuse = !rebuildAll
            rebuildAll = false
            val next = mutableMapOf<GiteaPRTimelineItemViewModel, BuiltItem>()
            items.forEach { item -> next.getOrPut(item) { (if (reuse) previous.remove(item) else null) ?: buildItem(item) } }
            // Whatever wasn't reused is off screen once the panel is patched below.
            previous.values.forEach { it.job.cancel() }
            itemComponents = next
            items.forEachIndexed { index, item ->
                val component = next.getValue(item).component
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
                        itemsPanel.removeAll()
                        dropItems()
                        itemsPanel.add(LoadingLabel().apply { border = CodeReviewTimelineUIUtil.ITEM_BORDER })
                    }
                    else -> res.fold(
                        onSuccess = { items -> renderItems(items) },
                        onFailure = { error ->
                            itemsPanel.removeAll()
                            dropItems()
                            // The platform's error styling, with Retry.
                            itemsPanel.add(
                                ErrorStatusPanelFactory.create(
                                    error,
                                    ErrorStatusPresenter.simple(
                                        GiteaBundle.message("pull.request.timeline.error"),
                                        descriptionProvider = { it.message },
                                        actionProvider = { swingAction(GiteaBundle.message("pull.request.error.retry")) { onRefresh() } },
                                    ),
                                    ErrorStatusPanelFactory.Alignment.LEFT,
                                ).apply { border = CodeReviewTimelineUIUtil.ITEM_BORDER },
                            )
                        },
                    )
                }
                itemsPanel.revalidate()
                itemsPanel.repaint()
            }
        }

        val commentField = JPanel(java.awt.BorderLayout()).apply {
            isOpaque = false
            border = EmptyBorder(ComponentType.FULL.inputPaddingInsets)
            add(commentFieldPanel(cs, vm, avatars), java.awt.BorderLayout.CENTER)
        }

        val column = VerticalListPanel(0).apply {
            border = JBUI.Borders.empty(CodeReviewTimelineUIUtil.VERT_PADDING, 0)
            add(titleLabel)
            add(description)
            add(itemsPanel)
            add(commentField)
        }

        val refreshBar = JPanel(java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 0, 0)).apply {
            isOpaque = false
            border = JBUI.Borders.empty(4, CodeReviewTimelineUIUtil.ITEM_HOR_PADDING)
            add(ActionLink(GiteaBundle.message("pull.request.timeline.refresh")) {
                // An explicit refresh rebuilds every item, so relative times ("5 minutes ago")
                // are re-rendered too; reloads triggered elsewhere keep reusing components.
                // The current components stay on screen until the reloaded items replace them.
                rebuildAll = true
                onRefresh()
            })
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

}

/** A timeline item's component and the job of the scope it was built in. */
private class BuiltItem(val component: JComponent, val job: Job)
