package com.github.jpmand.idea.plugin.gitea.pullrequest.ui.details

import com.github.jpmand.idea.plugin.gitea.pullrequest.ui.action.giteaWriteActionNotImplemented
import com.github.jpmand.idea.plugin.gitea.util.GiteaBundle
import com.intellij.collaboration.ui.HorizontalListPanel
import com.intellij.collaboration.ui.SimpleHtmlPane
import com.intellij.collaboration.ui.VerticalListPanel
import com.intellij.collaboration.ui.codereview.details.CodeReviewDetailsActionsComponentFactory
import com.intellij.collaboration.ui.codereview.details.CodeReviewDetailsBranchComponentFactory
import com.intellij.collaboration.ui.codereview.details.CodeReviewDetailsCommitInfoComponentFactory
import com.intellij.collaboration.ui.codereview.details.CodeReviewDetailsCommitsComponentFactory
import com.intellij.collaboration.ui.codereview.details.CodeReviewDetailsStatusComponentFactory
import com.intellij.collaboration.ui.codereview.details.CodeReviewDetailsTitleComponentFactory
import com.intellij.collaboration.ui.codereview.details.CommitPresentation
import com.intellij.collaboration.ui.codereview.details.ReviewDetailsUIUtil
import com.intellij.collaboration.ui.util.emptyBorders
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsSafe
import com.intellij.openapi.util.text.StringUtil
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.components.ActionLink
import com.intellij.util.ui.JBUI
import kotlinx.coroutines.CoroutineScope
import net.miginfocom.layout.CC
import net.miginfocom.layout.LC
import net.miginfocom.swing.MigLayout
import java.awt.event.ActionEvent
import java.util.Date
import javax.swing.AbstractAction
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.ScrollPaneConstants

/**
 * Read-only PR-details tool-window tab, laid out like the bundled GitLab plugin's
 * `GitLabMergeRequestDetailsComponentFactory`: title → nav bar → commits/branch → selected-commit
 * info → changes tree → status → write-action bar. Write actions are Milestone-2 stubs
 * (see [giteaWriteActionNotImplemented]); the changed-files tree and the conversation timeline
 * (opened via [onShowTimeline]) are the working surfaces.
 */
@Suppress("UnstableApiUsage")
class GiteaPRDetailsPanel(
    private val project: Project,
    private val cs: CoroutineScope,
    private val vm: GiteaPRDetailsViewModel,
    private val statusVm: GiteaPRStatusViewModel,
    private val changesComponent: JComponent,
    private val onShowTimeline: () -> Unit,
    private val onRefresh: () -> Unit,
) {

    fun create(): JComponent {
        val actionGroup = createActionGroup()

        val titleComponent = CodeReviewDetailsTitleComponentFactory.create(
            cs, vm,
            urlTooltip = GiteaBundle.message("pull.request.details.title.tooltip"),
            actionGroup = actionGroup,
            htmlPaneFactory = { SimpleHtmlPane() },
        )

        val navBar = HorizontalListPanel(8).apply {
            add(ActionLink(GiteaBundle.message("pull.request.action.show.timeline")) { onShowTimeline() })
            add(ActionLink(GiteaBundle.message("pull.request.action.refresh")) { onRefresh() })
        }

        val commitsAndBranch = HorizontalListPanel(0).apply {
            add(CodeReviewDetailsCommitsComponentFactory.create(cs, vm.changesVm) { commit -> commit.toPresentation() })
            add(CodeReviewDetailsBranchComponentFactory.create(cs, vm.branchesVm))
        }

        val commitInfo = CodeReviewDetailsCommitInfoComponentFactory.create(
            cs, vm.changesVm.selectedCommit,
            commitPresentation = { commit -> commit?.toPresentation() ?: emptyPresentation() },
            htmlPaneFactory = { SimpleHtmlPane() },
        )

        val statusComponent = VerticalListPanel(4).apply {
            add(CodeReviewDetailsStatusComponentFactory.createCiComponent(cs, statusVm))
            add(CodeReviewDetailsStatusComponentFactory.createNeedReviewerComponent(cs, statusVm.reviewerStates))
            add(CodeReviewDetailsStatusComponentFactory.createConflictsComponent(cs, statusVm.hasConflicts))
        }

        val actionsComponent = createActionsComponent()

        val content = VerticalListPanel(0).apply {
            border = JBUI.Borders.empty(8)
            add(pad(titleComponent, ReviewDetailsUIUtil.TITLE_GAPS.top, ReviewDetailsUIUtil.TITLE_GAPS.bottom))
            add(pad(navBar, 0, 8))
            add(pad(commitsAndBranch, 0, ReviewDetailsUIUtil.COMMIT_POPUP_BRANCHES_GAPS.bottom))
            add(pad(commitInfo, 0, ReviewDetailsUIUtil.COMMIT_INFO_GAPS.bottom))
        }

        return JPanel(MigLayout(LC().emptyBorders().fill().flowY().noGrid().gridGap("0", "0"))).apply {
            isOpaque = false
            add(ScrollPaneFactory.createScrollPane(content, true).apply {
                horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
            }, CC().growX())
            add(changesComponent, CC().grow().push())
            add(pad(statusComponent, ReviewDetailsUIUtil.STATUSES_GAPS.top, ReviewDetailsUIUtil.STATUSES_GAPS.bottom), CC().growX())
            add(pad(actionsComponent, ReviewDetailsUIUtil.ACTIONS_GAPS.top, ReviewDetailsUIUtil.ACTIONS_GAPS.bottom), CC().growX())
        }
    }

    // ── actions ────────────────────────────────────────────────────────────

    private fun createActionGroup(): ActionGroup = DefaultActionGroup().apply {
        add(object : AnAction(GiteaBundle.message("pull.request.action.open.in.browser")) {
            override fun actionPerformed(e: AnActionEvent) = BrowserUtil.browse(vm.url)
        })
    }

    private fun createActionsComponent(): JComponent {
        val openInBrowser = stubActionSwing("pull.request.action.open.in.browser") { BrowserUtil.browse(vm.url) }
        val reopen = stubActionSwing("pull.request.action.reopen")
        val readyForReview = stubActionSwing("pull.request.action.ready.for.review")

        val openedPanel = HorizontalListPanel(CodeReviewDetailsActionsComponentFactory.BUTTONS_GAP).apply {
            add(stubButton("pull.request.action.merge"))
            add(stubButton("pull.request.action.close"))
            add(stubButton("pull.request.action.submit.review"))
        }

        return CodeReviewDetailsActionsComponentFactory.createActionsComponent(
            cs, vm.reviewRequestState,
            openedStatePanel = openedPanel,
            mergedStatePanel = CodeReviewDetailsActionsComponentFactory.createActionsForMergedReview(),
            closedStatePanel = CodeReviewDetailsActionsComponentFactory.createActionsForClosedReview(reopen),
            draftedStatePanel = CodeReviewDetailsActionsComponentFactory.createActionsForDraftReview(readyForReview),
        )
    }

    private fun stubButton(bundleKey: String): JButton {
        val label = GiteaBundle.message(bundleKey)
        return JButton(label).apply { addActionListener { giteaWriteActionNotImplemented(project, label) } }
    }

    private fun stubActionSwing(bundleKey: String, action: (() -> Unit)? = null): AbstractAction {
        val label = GiteaBundle.message(bundleKey)
        return object : AbstractAction(label) {
            override fun actionPerformed(e: ActionEvent?) =
                action?.invoke() ?: giteaWriteActionNotImplemented(project, label)
        }
    }

    // ── helpers ────────────────────────────────────────────────────────────

    private fun com.github.jpmand.idea.plugin.gitea.api.rest.dto.Commit.toPresentation(): CommitPresentation {
        @NlsSafe val title = StringUtil.escapeXmlEntities(
            commit?.message?.lineSequence()?.firstOrNull()?.trim() ?: sha.orEmpty().take(7),
        )
        return CommitPresentation(
            titleHtml = title,
            descriptionHtml = "",
            author = commit?.author?.name ?: author?.login.orEmpty(),
            committedDate = created?.let { Date.from(it.toInstant()) } ?: Date(),
        )
    }

    private fun emptyPresentation() = CommitPresentation("", "", "", Date())

    private fun pad(c: JComponent, top: Int, bottom: Int): JComponent =
        JPanel(MigLayout(LC().emptyBorders().fillX().insets("$top", "0", "$bottom", "0"))).apply {
            isOpaque = false
            add(c, CC().growX().pushX())
        }
}
