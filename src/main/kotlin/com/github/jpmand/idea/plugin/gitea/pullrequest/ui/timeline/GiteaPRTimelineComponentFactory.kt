package com.github.jpmand.idea.plugin.gitea.pullrequest.ui.timeline

import com.github.jpmand.idea.plugin.gitea.api.models.GiteaReview
import com.github.jpmand.idea.plugin.gitea.api.models.GiteaUser
import com.github.jpmand.idea.plugin.gitea.api.rest.dto.CreatePullReviewOptions
import com.github.jpmand.idea.plugin.gitea.api.rest.dto.SubmitPullReviewOptions
import com.github.jpmand.idea.plugin.gitea.pullrequest.ui.comment.GiteaPRCommentFieldFactory
import com.github.jpmand.idea.plugin.gitea.util.GiteaBundle
import com.intellij.collaboration.ui.HorizontalListPanel
import com.intellij.collaboration.ui.VerticalListPanel
import com.intellij.collaboration.ui.icon.IconsProvider
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.components.ActionLink
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.panels.Wrapper
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.ScrollPaneConstants

/**
 * Assembles the PR activity-timeline editor, mirroring the bundled GitLab plugin's
 * `GitLabMergeRequestTimelineComponentFactory`: `[title, description, items, new-comment field,
 * review composer]` in a vertical column. The description is rendered through the same
 * [GiteaPRTimelineItemComponentFactory] shell as a synthetic comment.
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
        }

        val reviewComposer = JPanel(java.awt.BorderLayout()).apply {
            border = JBUI.Borders.empty(0, 16, 8, 16)
            add(reviewComposerPanel(cs, vm), java.awt.BorderLayout.CENTER)
        }

        val column = VerticalListPanel(0).apply {
            add(titleLabel)
            add(description)
            add(itemsPanel)
            add(commentField)
            add(reviewComposer)
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

    /**
     * Reactively swaps between the "start a review" composer and a "finish your review" prompt
     * depending on [GiteaPRTimelineViewModel.pendingReview] — mirrors the bundled GitHub plugin's
     * `GHPRFileEditorComponentFactory.createMergedTimelineItem`-style banner pattern, simplified:
     * a pending review here is body-only (no per-line diff-comment composition — see the plan
     * notes for why that's a separate, larger feature).
     */
    private fun reviewComposerPanel(cs: CoroutineScope, vm: GiteaPRTimelineViewModel): JComponent {
        val wrapper = Wrapper()
        cs.launch {
            vm.pendingReview.collect { pending ->
                wrapper.setContent(if (pending == null) startReviewPanel(cs, vm) else finishReviewPanel(cs, vm, pending))
                wrapper.revalidate()
                wrapper.repaint()
            }
        }
        return wrapper
    }

    private fun startReviewPanel(cs: CoroutineScope, vm: GiteaPRTimelineViewModel): JComponent {
        val textArea = reviewTextArea()
        val buttons = HorizontalListPanel(8).apply {
            add(JButton(GiteaBundle.message("pull.request.review.submit.comment")).apply {
                addActionListener { vm.submitReview(CreatePullReviewOptions.Event.COMMENT, textArea.text) }
            })
            add(JButton(GiteaBundle.message("pull.request.review.submit.approve")).apply {
                addActionListener { vm.submitReview(CreatePullReviewOptions.Event.APPROVED, textArea.text) }
            })
            add(JButton(GiteaBundle.message("pull.request.review.submit.request.changes")).apply {
                addActionListener { vm.submitReview(CreatePullReviewOptions.Event.REQUESTCHANGES, textArea.text) }
            })
            add(JButton(GiteaBundle.message("pull.request.timeline.review.save.pending")).apply {
                addActionListener { vm.submitReview(CreatePullReviewOptions.Event.PENDING, textArea.text) }
            })
        }
        bindBusyState(cs, vm, buttons)
        return VerticalListPanel(4).apply {
            add(JBLabel(GiteaBundle.message("pull.request.timeline.review.composer.hint")).apply {
                foreground = UIUtil.getContextHelpForeground()
                font = JBFont.small()
            })
            add(JBScrollPane(textArea))
            add(buttons)
        }
    }

    private fun finishReviewPanel(cs: CoroutineScope, vm: GiteaPRTimelineViewModel, pending: GiteaReview): JComponent {
        val textArea = reviewTextArea().apply { text = pending.body.orEmpty() }
        val buttons = HorizontalListPanel(8).apply {
            add(JButton(GiteaBundle.message("pull.request.review.submit.comment")).apply {
                addActionListener { vm.submitPendingReview(SubmitPullReviewOptions.Event.COMMENT, textArea.text) }
            })
            add(JButton(GiteaBundle.message("pull.request.review.submit.approve")).apply {
                addActionListener { vm.submitPendingReview(SubmitPullReviewOptions.Event.APPROVED, textArea.text) }
            })
            add(JButton(GiteaBundle.message("pull.request.review.submit.request.changes")).apply {
                addActionListener { vm.submitPendingReview(SubmitPullReviewOptions.Event.REQUESTCHANGES, textArea.text) }
            })
        }
        bindBusyState(cs, vm, buttons)
        return VerticalListPanel(4).apply {
            add(JBLabel(GiteaBundle.message("pull.request.timeline.review.pending.banner")).apply {
                font = JBFont.label().asBold()
            })
            add(JBScrollPane(textArea))
            add(buttons)
        }
    }

    private fun reviewTextArea(): JBTextArea =
        JBTextArea(3, 40).apply {
            lineWrap = true
            wrapStyleWord = true
        }

    /** [JComponent.setEnabled] doesn't propagate to children in Swing — disable each button
     * directly so the whole row is inert while a review submission is in flight. */
    private fun bindBusyState(cs: CoroutineScope, vm: GiteaPRTimelineViewModel, buttons: JComponent) {
        cs.launch {
            vm.isSubmittingReview.collect { busy -> buttons.components.forEach { it.isEnabled = !busy } }
        }
    }
}
