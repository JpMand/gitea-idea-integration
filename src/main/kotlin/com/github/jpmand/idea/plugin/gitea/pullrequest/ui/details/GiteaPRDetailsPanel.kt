package com.github.jpmand.idea.plugin.gitea.pullrequest.ui.details

import com.github.jpmand.idea.plugin.gitea.api.models.GiteaReview
import com.github.jpmand.idea.plugin.gitea.api.rest.dto.CreatePullReviewOptions
import com.github.jpmand.idea.plugin.gitea.api.rest.dto.MergePullRequestOption
import com.github.jpmand.idea.plugin.gitea.api.rest.dto.SubmitPullReviewOptions
import com.github.jpmand.idea.plugin.gitea.pullrequest.review.GiteaPRDiscussionsViewModels
import com.github.jpmand.idea.plugin.gitea.pullrequest.ui.action.giteaWriteActionNotImplemented
import com.github.jpmand.idea.plugin.gitea.pullrequest.ui.confirmAndCancelReview
import com.github.jpmand.idea.plugin.gitea.util.GiteaBundle
import com.github.jpmand.idea.plugin.gitea.util.GiteaUtil
import com.intellij.collaboration.ui.Either
import com.intellij.collaboration.ui.HorizontalListPanel
import com.intellij.collaboration.ui.SimpleHtmlPane
import com.intellij.collaboration.ui.VerticalListPanel
import com.intellij.collaboration.ui.codereview.details.*
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsSafe
import com.intellij.openapi.util.text.StringUtil
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.components.*
import com.intellij.ui.components.panels.Wrapper
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import net.miginfocom.layout.CC
import net.miginfocom.layout.LC
import net.miginfocom.swing.MigLayout
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.util.*
import javax.swing.*

/**
 * PR-details tool-window tab, laid out like the bundled GitLab plugin's
 * `GitLabMergeRequestDetailsComponentFactory`: title → nav bar → commits/branch → selected-commit
 * info → changes tree → status → write-action bar → review composer. Every action in the
 * write-action bar (open in browser, close/reopen, ready-for-review, merge) and the review
 * composer are wired to real API calls; [giteaWriteActionNotImplemented] is only the fallback
 * [stubActionSwing] takes when a caller doesn't pass an action, kept around for any future stub.
 */
@Suppress("UnstableApiUsage")
class GiteaPRDetailsPanel(
    private val project: Project,
    private val cs: CoroutineScope,
    private val vm: GiteaPRDetailsViewModel,
    private val statusVm: GiteaPRStatusViewModel,
    private val discussionsVm: GiteaPRDiscussionsViewModels,
    private val changesComponent: JComponent,
    private val onShowTimeline: () -> Unit,
    private val onRefresh: () -> Unit,
) {

    companion object {
        /** Tighter than the platform's default [CodeReviewDetailsActionsComponentFactory.BUTTONS_GAP]/
         * [ReviewDetailsUIUtil.ACTIONS_GAPS] — this row (merge control, delete-branch checkbox,
         * close button) reads better compact. */
        private val COMPACT_BUTTONS_GAP = JBUI.scale(4)
        private val COMPACT_ACTIONS_GAP = JBUI.scale(4)

        /** Cap on the "Show details" commit-info area — past this it scrolls internally instead
         * of pushing the changes tree/status/actions rows down or off-screen. */
        private const val COMMIT_INFO_MAX_HEIGHT = 220
    }

    fun create(): JComponent {
        val actionGroup = createActionGroup()

        val titleComponent = CodeReviewDetailsTitleComponentFactory.create(
            cs, vm,
            urlTooltip = GiteaBundle.message("pull.request.details.title.tooltip"),
            actionGroup = actionGroup,
            htmlPaneFactory = { SimpleHtmlPane() },
        )

        val navBar = JPanel(java.awt.BorderLayout()).apply {
            isOpaque = false
            add(ActionLink(GiteaBundle.message("pull.request.action.show.timeline")) { onShowTimeline() }, java.awt.BorderLayout.WEST)
            add(ActionLink(GiteaBundle.message("pull.request.action.refresh")) { onRefresh() }, java.awt.BorderLayout.EAST)
        }

        val commitsAndBranch = JPanel(java.awt.BorderLayout()).apply {
            isOpaque = false
            add(
                CodeReviewDetailsCommitsComponentFactory.create(cs, vm.changesVm) { commit -> commit.toPresentation() },
                java.awt.BorderLayout.WEST,
            )
            add(CodeReviewDetailsBranchComponentFactory.create(cs, vm.branchesVm), java.awt.BorderLayout.EAST)
        }

        // The platform factory bundles its own "Show details" toggle that reveals the selected
        // commit's full message — can get long, so it gets its own capped, independently
        // scrolling area instead of sharing one with (and potentially pushing off-screen) the
        // title/nav-bar/branch row above it.
        val commitInfo = CodeReviewDetailsCommitInfoComponentFactory.create(
            cs, vm.changesVm.selectedCommit,
            commitPresentation = { commit -> commit.toPresentation() },
            htmlPaneFactory = { SimpleHtmlPane() },
        )

        val resolveConflictsLink = MutableStateFlow<Either<String, ActionListener>>(
            Either.right(ActionListener { vm.branchesVm.resolveConflicts() }),
        )
        val statusComponent = VerticalListPanel(4).apply {
            add(CodeReviewDetailsStatusComponentFactory.createCiComponent(cs, statusVm))
            add(CodeReviewDetailsStatusComponentFactory.createNeedReviewerComponent(cs, statusVm.reviewerStates))
            add(
                CodeReviewDetailsStatusComponentFactory.createConflictsComponent(
                    cs, statusVm.hasConflicts, resolveConflictsLink, vm.branchesVm.isResolvingConflicts,
                ),
            )
        }

        val actionsComponent = createActionsComponent()
        val reviewComponent = reviewComposerPanel(cs, discussionsVm)

        // Title/nav-bar/branch row: always fully visible, never scrolls on its own.
        val header = VerticalListPanel(0).apply {
            border = JBUI.Borders.empty(8, 8, 0, 8)
            add(pad(titleComponent, ReviewDetailsUIUtil.TITLE_GAPS.top, ReviewDetailsUIUtil.TITLE_GAPS.bottom))
            add(pad(navBar, 0, 8))
            add(pad(commitsAndBranch, 0, 4))
        }

        val commitInfoScrollPane = ScrollPaneFactory.createScrollPane(pad(commitInfo, 0, 0), true).apply {
            horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
        }

        // shrink(0) on the header/commit-info rows keeps them at their preferred size always —
        // the changes tree (the sole push/grow row) is what absorbs a shrinking tool-window
        // height first; only once it's already at its own minimum does MigLayout start shrinking
        // the remaining non-push rows (status, then actions).
        return JPanel(MigLayout(LC().insets("0").fill().flowY().noGrid().gridGap("0", "0"))).apply {
            isOpaque = false
            add(header, CC().growX().shrink(0f))
            add(commitInfoScrollPane, CC().growX().shrink(0f).maxHeight("${JBUI.scale(COMMIT_INFO_MAX_HEIGHT)}"))
            add(changesComponent, CC().grow().push())
            add(pad(statusComponent, ReviewDetailsUIUtil.STATUSES_GAPS.top, ReviewDetailsUIUtil.STATUSES_GAPS.bottom), CC().growX())
            add(pad(actionsComponent, COMPACT_ACTIONS_GAP, COMPACT_ACTIONS_GAP), CC().growX())
            add(pad(reviewComponent, COMPACT_ACTIONS_GAP, COMPACT_ACTIONS_GAP), CC().growX())
        }
    }

    // ── review submission ─────────────────────────────────────────────────

    /**
     * Reactively swaps between the "start a review" composer and a "finish your review" prompt
     * depending on [GiteaPRDiscussionsViewModels.pendingReview] — a pending review here is
     * body+verdict only (no per-line composition; that happens in the diff editor, which is also
     * where [GiteaPRDiscussionsViewModels.draftComments] gets populated).
     */
    private fun reviewComposerPanel(cs: CoroutineScope, discussionsVm: GiteaPRDiscussionsViewModels): JComponent {
        val wrapper = Wrapper()
        cs.launch {
            discussionsVm.pendingReview.collect { pending ->
                wrapper.setContent(
                    if (pending == null) startReviewPanel(cs, discussionsVm) else finishReviewPanel(cs, discussionsVm, pending),
                )
                wrapper.revalidate()
                wrapper.repaint()
            }
        }
        return wrapper
    }

    private fun startReviewPanel(cs: CoroutineScope, discussionsVm: GiteaPRDiscussionsViewModels): JComponent {
        val textArea = reviewTextArea()
        val draftCountLabel = JBLabel().apply {
            foreground = UIUtil.getContextHelpForeground()
            font = JBFont.small()
        }
        val cancelButton = JButton(GiteaBundle.message("pull.request.action.cancel.review")).apply {
            addActionListener { confirmAndCancelReview(project, discussionsVm) }
        }
        cs.launch {
            discussionsVm.draftComments.collect { drafts ->
                draftCountLabel.text = GiteaBundle.message("pull.request.review.composer.draft.count", drafts.size)
                cancelButton.isVisible = drafts.isNotEmpty()
            }
        }
        val buttons = HorizontalListPanel(COMPACT_BUTTONS_GAP).apply {
            add(JButton(GiteaBundle.message("pull.request.action.comment")).apply {
                addActionListener { discussionsVm.submitReview(CreatePullReviewOptions.Event.COMMENT, textArea.text) }
            })
            add(JButton(GiteaBundle.message("pull.request.action.approve")).apply {
                addActionListener { discussionsVm.submitReview(CreatePullReviewOptions.Event.APPROVED, textArea.text) }
            })
            add(JButton(GiteaBundle.message("pull.request.action.request.changes")).apply {
                addActionListener { discussionsVm.submitReview(CreatePullReviewOptions.Event.REQUESTCHANGES, textArea.text) }
            })
            add(JButton(GiteaBundle.message("pull.request.review.save.pending")).apply {
                addActionListener { discussionsVm.submitReview(CreatePullReviewOptions.Event.PENDING, textArea.text) }
            })
            add(cancelButton)
        }
        bindBusyState(cs, discussionsVm, buttons)
        return VerticalListPanel(4).apply {
            add(draftCountLabel)
            add(JBScrollPane(textArea))
            add(buttons)
        }
    }

    private fun finishReviewPanel(cs: CoroutineScope, discussionsVm: GiteaPRDiscussionsViewModels, pending: GiteaReview): JComponent {
        val textArea = reviewTextArea().apply { text = pending.body.orEmpty() }
        val buttons = HorizontalListPanel(COMPACT_BUTTONS_GAP).apply {
            add(JButton(GiteaBundle.message("pull.request.action.comment")).apply {
                addActionListener { discussionsVm.submitPendingReview(SubmitPullReviewOptions.Event.COMMENT, textArea.text) }
            })
            add(JButton(GiteaBundle.message("pull.request.action.approve")).apply {
                addActionListener { discussionsVm.submitPendingReview(SubmitPullReviewOptions.Event.APPROVED, textArea.text) }
            })
            add(JButton(GiteaBundle.message("pull.request.action.request.changes")).apply {
                addActionListener { discussionsVm.submitPendingReview(SubmitPullReviewOptions.Event.REQUESTCHANGES, textArea.text) }
            })
            add(JButton(GiteaBundle.message("pull.request.action.cancel.review")).apply {
                addActionListener { confirmAndCancelReview(project, discussionsVm) }
            })
        }
        bindBusyState(cs, discussionsVm, buttons)
        return VerticalListPanel(4).apply {
            add(JBLabel(GiteaBundle.message("pull.request.review.pending.banner")).apply { font = JBFont.label().asBold() })
            add(JBScrollPane(textArea))
            add(buttons)
        }
    }

    private fun reviewTextArea(): JBTextArea = JBTextArea(3, 40).apply { lineWrap = true; wrapStyleWord = true }

    /** [JComponent.setEnabled] doesn't propagate to children in Swing — disable each button
     * directly so the whole row is inert while a review submission is in flight. */
    private fun bindBusyState(cs: CoroutineScope, discussionsVm: GiteaPRDiscussionsViewModels, buttons: JComponent) {
        cs.launch {
            discussionsVm.isSubmittingReview.collect { busy -> buttons.components.forEach { it.isEnabled = !busy } }
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
        val reopen = stubActionSwing("pull.request.action.reopen") { vm.reopenPullRequest() }
        val readyForReview = stubActionSwing("pull.request.action.ready.for.review") { vm.markReadyForReview() }
        val closeButton = actionButton("pull.request.action.close") { vm.closePullRequest() }
        val (mergeControl, mergeOptionButton) = createMergeControl()

        val openedPanel = HorizontalListPanel(COMPACT_BUTTONS_GAP).apply {
            add(mergeControl)
            add(closeButton)
        }

        // Mirrors the bundled GitHub plugin's isBusy-gated close/reopen/merge actions: disable
        // while a call is in flight so a double-click can't fire concurrent requests.
        cs.launch {
            vm.isActionInProgress.collect { busy ->
                closeButton.isEnabled = !busy
                mergeOptionButton.isEnabled = !busy
                reopen.isEnabled = !busy
                readyForReview.isEnabled = !busy
            }
        }

        return CodeReviewDetailsActionsComponentFactory.createActionsComponent(
            cs, vm.reviewRequestState,
            openedStatePanel = openedPanel,
            mergedStatePanel = CodeReviewDetailsActionsComponentFactory.createActionsForMergedReview(),
            closedStatePanel = CodeReviewDetailsActionsComponentFactory.createActionsForClosedReview(reopen),
            draftedStatePanel = CodeReviewDetailsActionsComponentFactory.createActionsForDraftReview(readyForReview),
        )
    }

    private fun actionButton(bundleKey: String, action: () -> Unit): JButton =
        JButton(GiteaBundle.message(bundleKey)).apply { addActionListener { action() } }

    /**
     * A GitHub-style "Merge ▾" split button — the default action merges immediately, the dropdown
     * offers the other strategies, no confirmation dialog. Reference: `GHPRCommitMergeAction` /
     * `GHPRSquashMergeAction` in the bundled GitHub plugin, each a plain per-strategy action with
     * no intermediate dialog step.
     */
    private fun createMergeControl(): Pair<JComponent, JBOptionButton> {
        val deleteBranchCheckBox = JBCheckBox(GiteaBundle.message("pull.request.merge.dialog.delete.branch"))
        val strategies = listOf(
            MergePullRequestOption.Do.MERGE,
            MergePullRequestOption.Do.SQUASH,
            MergePullRequestOption.Do.REBASE,
            MergePullRequestOption.Do.REBASEMERGE,
            MergePullRequestOption.Do.FASTFORWARDONLY,
        )
        fun mergeAction(method: MergePullRequestOption.Do) = object : AbstractAction(mergeMethodLabel(method)) {
            override fun actionPerformed(e: ActionEvent?) = vm.mergePullRequest(method, deleteBranchCheckBox.isSelected)
        }

        val optionButton = JBOptionButton(
            mergeAction(strategies.first()),
            strategies.drop(1).map { mergeAction(it) }.toTypedArray(),
        )

        val panel = HorizontalListPanel(COMPACT_BUTTONS_GAP).apply {
            add(optionButton)
            add(deleteBranchCheckBox)
        }
        return panel to optionButton
    }

    private fun mergeMethodLabel(method: MergePullRequestOption.Do): String = when (method) {
        MergePullRequestOption.Do.MERGE -> GiteaBundle.message("pull.request.merge.method.merge")
        MergePullRequestOption.Do.REBASE -> GiteaBundle.message("pull.request.merge.method.rebase")
        MergePullRequestOption.Do.REBASEMERGE -> GiteaBundle.message("pull.request.merge.method.rebase.merge")
        MergePullRequestOption.Do.SQUASH -> GiteaBundle.message("pull.request.merge.method.squash")
        MergePullRequestOption.Do.FASTFORWARDONLY -> GiteaBundle.message("pull.request.merge.method.fast.forward")
        MergePullRequestOption.Do.MANUALLYMERGED -> method.value
    }

    private fun stubActionSwing(bundleKey: String, action: (() -> Unit)? = null): AbstractAction {
        val label = GiteaBundle.message(bundleKey)
        return object : AbstractAction(label) {
            override fun actionPerformed(e: ActionEvent?) =
                action?.invoke() ?: giteaWriteActionNotImplemented(project, label)
        }
    }

    // ── helpers ────────────────────────────────────────────────────────────

    private fun com.github.jpmand.idea.plugin.gitea.api.models.GiteaCommit.toPresentation(): CommitPresentation {
        @NlsSafe val title = StringUtil.escapeXmlEntities(messageTitle)
        return CommitPresentation(
            titleHtml = GiteaUtil.safeConvertMarkdownToHtml(title),
            descriptionHtml = GiteaUtil.safeConvertMarkdownToHtml(messageBody.orEmpty()),
            author = authorName ?: author?.login.orEmpty(),
            committedDate = createdAt ?: Date(),
        )
    }

    private fun pad(c: JComponent, top: Int, bottom: Int): JComponent =
        JPanel(MigLayout(LC().fillX().insets("$top", "0", "$bottom", "0"))).apply {
            isOpaque = false
            add(c, CC().growX().pushX())
        }
}
