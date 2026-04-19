package com.github.jpmand.idea.plugin.gitea.pullrequest.ui.details

import com.github.jpmand.idea.plugin.gitea.util.GiteaBundle
import com.intellij.collaboration.ui.SimpleHtmlPane
import com.intellij.collaboration.ui.VerticalListPanel
import kotlinx.coroutines.flow.StateFlow
import com.intellij.collaboration.ui.codereview.details.CodeReviewDetailsBranchComponentFactory
import com.intellij.collaboration.ui.codereview.details.CodeReviewDetailsCommitsComponentFactory
import com.intellij.collaboration.ui.codereview.details.CodeReviewDetailsDescriptionComponentFactory
import com.intellij.collaboration.ui.codereview.details.CodeReviewDetailsTitleComponentFactory
import com.intellij.collaboration.ui.codereview.details.CodeReviewDetailsActionsComponentFactory
import com.intellij.collaboration.ui.codereview.details.CommitPresentation
import com.intellij.collaboration.ui.util.emptyBorders
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.util.NlsSafe
import com.intellij.openapi.util.text.StringUtil
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.components.ActionLink
import com.intellij.util.ui.JBUI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import net.miginfocom.layout.AC
import net.miginfocom.layout.CC
import net.miginfocom.layout.LC
import net.miginfocom.swing.MigLayout
import java.awt.event.ActionEvent
import javax.swing.AbstractAction
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.ScrollPaneConstants

@Suppress("UnstableApiUsage")
class GiteaPRDetailsPanel(
    private val cs: CoroutineScope,
    private val vm: GiteaPRDetailsViewModel,
    private val onBack: () -> Unit,
    private val onViewChanges: (() -> Unit)? = null,
    private val onRefresh: (() -> Unit)? = null,
    private val onSubmitReview: (suspend (JComponent) -> Unit)? = null,
    private val draftCommentsCount: StateFlow<Int>? = null,
) {

    fun create(): JComponent {
        val backLink = ActionLink(GiteaBundle.message("pull.request.details.back")) { onBack() }.apply {
            border = JBUI.Borders.empty(4, 8)
        }

        val viewChangesLink = onViewChanges?.let {
            ActionLink(GiteaBundle.message("pull.request.action.view.changes")) { it() }.apply {
                border = JBUI.Borders.empty(4, 8)
            }
        }

        val refreshLink = onRefresh?.let {
            ActionLink(GiteaBundle.message("pull.request.action.refresh")) { it() }.apply {
                border = JBUI.Borders.empty(4, 8)
            }
        }

        val submitReviewButton = onSubmitReview?.let { submitFn ->
            val button = JButton(GiteaBundle.message("pull.request.review.submit.action")).apply {
                border = JBUI.Borders.empty(4, 8)
                isOpaque = false
            }
            button.addActionListener {
                button.isEnabled = false
                cs.launch {
                    try {
                        submitFn(button)
                    } finally {
                        button.isEnabled = true
                    }
                }
            }
            if (draftCommentsCount != null) {
                cs.launch {
                    draftCommentsCount.collect { count ->
                        button.text = if (count > 0)
                            GiteaBundle.message("pull.request.review.submit.action.with.count", count)
                        else
                            GiteaBundle.message("pull.request.review.submit.action")
                    }
                }
            }
            button
        }
        val actionGroup = createActionGroup()

        val titleComponent = CodeReviewDetailsTitleComponentFactory.create(
            cs, vm,
            urlTooltip = GiteaBundle.message("pull.request.details.title.tooltip"),
            actionGroup = actionGroup,
            htmlPaneFactory = { SimpleHtmlPane() },
        )

        val branchesAndCommits = JPanel(MigLayout(LC().emptyBorders().fill(), AC().gap("push"))).apply {
            isOpaque = false
            add(CodeReviewDetailsCommitsComponentFactory.create(cs, vm.changesVm) { commit ->
                @NlsSafe val title = StringUtil.escapeXmlEntities(
                    commit.commit.message.lines().firstOrNull()?.trim() ?: commit.sha.take(7)
                )
                CommitPresentation(
                    titleHtml = title,
                    descriptionHtml = "",
                    author = commit.commit.author.name,
                    committedDate = commit.created,
                )
            })
            add(CodeReviewDetailsBranchComponentFactory.create(cs, vm.branchesVm))
        }

        val actionsComponent = createActionsComponent()

        val content = VerticalListPanel(8).apply {
            add(titleComponent)
            vm.description?.let {
                val descriptionComponent = CodeReviewDetailsDescriptionComponentFactory.create(
                    cs, vm,
                    actionGroup = actionGroup,
                    showTimelineAction = { /* Phase 10: open timeline */ },
                    htmlPaneFactory = { SimpleHtmlPane() },
                )
                add(descriptionComponent)
            }
            add(branchesAndCommits)
            add(actionsComponent)
        }

        val scrollPane = ScrollPaneFactory.createScrollPane(content, true).apply {
            horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
        }

        return JPanel(MigLayout(LC().emptyBorders().fill().flowY().noGrid())).apply {
            isOpaque = false
            val navBar = JPanel(MigLayout(LC().emptyBorders().fill().noGrid(), AC().gap("push"))).apply {
                isOpaque = false
                add(backLink)
                viewChangesLink?.let { add(it) }
                refreshLink?.let { add(it) }
                submitReviewButton?.let { add(it) }
            }
            add(navBar, CC().growX())
            add(scrollPane, CC().grow().push())
        }
    }

    private fun createActionGroup(): ActionGroup = DefaultActionGroup().apply {
        add(object : AnAction(GiteaBundle.message("pull.request.action.open.in.browser")) {
            override fun actionPerformed(e: AnActionEvent) {
                BrowserUtil.browse(vm.url)
            }
        })
    }

    private fun createActionsComponent(): JComponent {
        val mergeAction = object : AbstractAction(GiteaBundle.message("pull.request.action.merge")) {
            override fun actionPerformed(e: ActionEvent?) { vm.merge() }
        }
        val closeAction = object : AbstractAction(GiteaBundle.message("pull.request.action.close")) {
            override fun actionPerformed(e: ActionEvent?) { vm.close() }
        }
        val reopenAction = object : AbstractAction(GiteaBundle.message("pull.request.action.reopen")) {
            override fun actionPerformed(e: ActionEvent?) { vm.reopen() }
        }
        // Draft PRs: open in browser (Gitea v1 API has no direct draft→ready endpoint)
        val openInBrowserAction = object : AbstractAction(GiteaBundle.message("pull.request.action.open.in.browser")) {
            override fun actionPerformed(e: ActionEvent?) { BrowserUtil.browse(vm.url) }
        }

        val openedPanel = JPanel(MigLayout(LC().emptyBorders().fill().noGrid(), AC().gap("4"))).apply {
            isOpaque = false
            add(JButton(mergeAction).apply { isOpaque = false })
            add(JButton(closeAction).apply { isOpaque = false })
        }

        return CodeReviewDetailsActionsComponentFactory.createActionsComponent(
            cs, vm.reviewRequestState,
            openedStatePanel = openedPanel,
            mergedStatePanel = CodeReviewDetailsActionsComponentFactory.createActionsForMergedReview(),
            closedStatePanel = CodeReviewDetailsActionsComponentFactory.createActionsForClosedReview(reopenAction),
            draftedStatePanel = CodeReviewDetailsActionsComponentFactory.createActionsForDraftReview(openInBrowserAction),
        )
    }
}

