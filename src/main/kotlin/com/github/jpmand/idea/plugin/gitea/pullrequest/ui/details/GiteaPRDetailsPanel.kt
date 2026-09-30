package com.github.jpmand.idea.plugin.gitea.pullrequest.ui.details

import com.github.jpmand.idea.plugin.gitea.api.rest.dto.MergePullRequestOption
import com.github.jpmand.idea.plugin.gitea.pullrequest.review.GiteaPRDiscussionsViewModels
import com.github.jpmand.idea.plugin.gitea.pullrequest.ui.action.giteaWriteActionNotImplemented
import com.github.jpmand.idea.plugin.gitea.util.GiteaBundle
import com.github.jpmand.idea.plugin.gitea.util.GiteaUtil
import com.intellij.collaboration.ui.Either
import com.intellij.collaboration.ui.HorizontalListPanel
import com.intellij.collaboration.ui.ScrollablePanel
import com.intellij.collaboration.ui.SimpleHtmlPane
import com.intellij.collaboration.ui.VerticalListPanel
import com.intellij.collaboration.ui.codereview.avatar.CodeReviewAvatarUtils
import com.intellij.collaboration.ui.codereview.details.*
import com.intellij.ide.BrowserUtil
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsSafe
import com.intellij.openapi.util.text.StringUtil
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.components.*
import com.intellij.util.ui.JBUI
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
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
 * info → changes tree → status → write-action bar. Every action in the write-action bar (open
 * in browser, close/reopen, ready-for-review, merge) is wired to a real API call;
 * [giteaWriteActionNotImplemented] is only the fallback [stubActionSwing] takes when a caller
 * doesn't pass an action, kept around for any future stub. Reviews are submitted from the
 * diff/editor review toolbar, not from here.
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
        // Unscaled on purpose: HorizontalListPanel scales its gap itself.
        private const val COMPACT_BUTTONS_GAP = CodeReviewDetailsActionsComponentFactory.BUTTONS_GAP

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
        // Each status row carries its own vertical padding, so no extra gap between them.
        val statusRows = VerticalListPanel(0).apply {
            add(CodeReviewDetailsStatusComponentFactory.createCiComponent(cs, statusVm))
            add(
                CodeReviewDetailsStatusComponentFactory.createConflictsComponent(
                    cs, statusVm.hasConflicts, resolveConflictsLink, vm.branchesVm.isResolvingConflicts,
                ),
            )
            add(CodeReviewDetailsStatusComponentFactory.createNeedReviewerComponent(cs, statusVm.reviewerStates))
            // One row per reviewer (avatar outlined by state + "approved"/"requested changes"), as in GitHub.
            add(
                CodeReviewDetailsStatusComponentFactory.createReviewersReviewStateComponent(
                    cs, statusVm.reviewerStates,
                    reviewerActionProvider = { null },
                    reviewerNameProvider = { user -> user.fullName ?: user.login },
                    avatarKeyProvider = { user -> user },
                    iconProvider = { state, user, size ->
                        CodeReviewAvatarUtils.createIconWithOutline(
                            discussionsVm.avatars.getIcon(user, size), ReviewDetailsUIUtil.getReviewStateIconBorder(state),
                        )
                    },
                ),
            )
        }
        val statusComponent = ScrollPaneFactory.createScrollPane(statusRows, true).apply {
            isOpaque = false
            viewport.isOpaque = false
            horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
        }

        val actionsComponent = createActionsComponent()

        // Title with its links underneath, 8px apart, like the platform's ReviewDetailsUIUtil.createTitlePanel.
        val titlePanel = VerticalListPanel(8).apply {
            isOpaque = false
            add(titleComponent)
            add(navBar)
        }

        // The view must track the viewport's width: otherwise a long commit message lays out at
        // its full unwrapped width, clipping the text and pushing "Hide details" out of view.
        val commitInfoView = ScrollablePanel(SwingConstants.VERTICAL, java.awt.BorderLayout()).apply {
            isOpaque = false
            add(commitInfo, java.awt.BorderLayout.CENTER)
        }
        val commitInfoScrollPane = ScrollPaneFactory.createScrollPane(commitInfoView, true).apply {
            horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
        }

        // Sections are spaced with the platform's review-details gaps (16 left, 14 right), the
        // same layout as the GitHub plugin's details panel. shrinkY(0) on the header/commit-info
        // rows keeps them at their preferred height always — the changes tree (the sole push/grow
        // row) is what absorbs a shrinking tool-window height first.
        return JPanel(MigLayout(LC().insets("0").fill().flowY().noGrid().hideMode(3))).apply {
            isOpaque = false
            // Only vertically: a row that can't shrink horizontally lays out at its unwrapped width.
            add(titlePanel, CC().growX().shrinkY(0f).gaps(ReviewDetailsUIUtil.TITLE_GAPS))
            add(commitsAndBranch, CC().growX().shrinkY(0f).gaps(ReviewDetailsUIUtil.COMMIT_POPUP_BRANCHES_GAPS))
            add(
                commitInfoScrollPane,
                CC().growX().shrinkY(0f).maxHeight("${JBUI.scale(COMMIT_INFO_MAX_HEIGHT)}").gaps(ReviewDetailsUIUtil.COMMIT_INFO_GAPS),
            )
            add(changesComponent, CC().grow().push().shrinkPrioY(200))
            add(statusComponent, CC().growX().maxHeight("${ReviewDetailsUIUtil.STATUSES_MAX_HEIGHT}").gaps(ReviewDetailsUIUtil.STATUSES_GAPS))
            add(actionsComponent, CC().growX().minHeight("pref").gaps(ReviewDetailsUIUtil.ACTIONS_GAPS))
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
        // A separate instance: a Swing component can only sit in one of the state panels.
        val closeDraftButton = actionButton("pull.request.action.close") { vm.closePullRequest() }.apply { isOpaque = false }
        val requestReviewButton = createRequestReviewButton()
        val (mergeControl, mergeOptionButton) = createMergeControl()


        val actionPanel = VerticalListPanel().apply {
            add(HorizontalListPanel(COMPACT_BUTTONS_GAP).apply {
                add(mergeControl)
            })
            add(HorizontalListPanel(COMPACT_BUTTONS_GAP).apply {
                add(requestReviewButton)
                add(closeButton)
            })
        }

        // Mirrors the bundled GitHub plugin's isBusy-gated close/reopen/merge actions: disable
        // while a call is in flight so a double-click can't fire concurrent requests. Merge is
        // additionally gated on statusVm.hasConflicts (Gitea's mergeable flag OR'd with the local
        // merge-tree check, see GiteaPRBranchesViewModel.localMergeabilityState) so a doomed merge
        // can't even be attempted.
        cs.launch {
            vm.isActionInProgress.combine(statusVm.hasConflicts) { busy, hasConflicts -> busy to hasConflicts }
                .collect { (busy, hasConflicts) ->
                    closeButton.isEnabled = !busy
                    closeDraftButton.isEnabled = !busy
                    requestReviewButton.isEnabled = !busy
                    mergeOptionButton.isEnabled = !busy && !hasConflicts
                    reopen.isEnabled = !busy
                    readyForReview.isEnabled = !busy
                }
        }

        return CodeReviewDetailsActionsComponentFactory.createActionsComponent(
            cs, vm.reviewRequestState,
            openedStatePanel = actionPanel,
            mergedStatePanel = CodeReviewDetailsActionsComponentFactory.createActionsForMergedReview(),
            closedStatePanel = CodeReviewDetailsActionsComponentFactory.createActionsForClosedReview(reopen),
            // The platform's draft panel only offers "Ready for Review"; a draft can be closed too.
            draftedStatePanel = HorizontalListPanel(COMPACT_BUTTONS_GAP).apply {
                add(CodeReviewDetailsActionsComponentFactory.createActionsForDraftReview(readyForReview))
                add(closeDraftButton)
            },
        )
    }

    private fun actionButton(bundleKey: String, action: () -> Unit): JButton =
        JButton(GiteaBundle.message(bundleKey)).apply {
            isOpaque = false
            addActionListener { action() }
        }

    /**
     * Loads candidate reviewers (respecting the per-account "list all users" setting, see
     * [GiteaPRDetailsViewModel.loadPossibleReviewers]), shows [showReviewersPicker] anchored under
     * the button, and applies the resulting add/remove delta via [GiteaPRDetailsViewModel.requestReview].
     * A no-op (no API call) if the picker is dismissed without changes.
     */
    private fun createRequestReviewButton(): JButton {
        lateinit var button: JButton
        button = JButton(GiteaBundle.message("pull.request.action.request.review")).apply {
            isOpaque = false
            addActionListener {
                cs.launch {
                    try {
                        val candidates = vm.loadPossibleReviewers()
                        val delta = showReviewersPicker(RelativePoint.getSouthWestOf(button), candidates, vm.currentlyRequestedReviewers)
                            ?: return@launch
                        if (!delta.isEmpty) {
                            vm.requestReview(delta.newItems.map { it.login }, delta.removedItems.map { it.login })
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        NotificationGroupManager.getInstance()
                            .getNotificationGroup("Gitea")
                            .createNotification(GiteaBundle.message("pull.request.action.request.review.load.error"), NotificationType.ERROR)
                            .notify(project)
                    }
                }
            }
        }
        return button
    }

    /**
     * A GitHub-style "Merge ▾" split button (see [createSelectableOptionButton] for the
     * select-then-confirm mechanics) — picking a strategy from the dropdown only changes what the
     * primary button will do; only clicking the primary button itself merges. Reference:
     * `GHPRCommitMergeAction`/`GHPRSquashMergeAction` in the bundled GitHub plugin for the
     * per-strategy action shape (that plugin fires immediately per-action; this one adds the
     * select/confirm split on top).
     */
    private fun createMergeControl(): Pair<JComponent, JBOptionButton> {
        val deleteBranchCheckBox = JBCheckBox(GiteaBundle.message("pull.request.merge.dialog.delete.branch")).apply { isOpaque = false }
        val strategies = listOf(
            MergePullRequestOption.Do.MERGE,
            MergePullRequestOption.Do.SQUASH,
            MergePullRequestOption.Do.REBASE,
            MergePullRequestOption.Do.REBASEMERGE,
            MergePullRequestOption.Do.FASTFORWARDONLY,
        )
        val optionButton = createSelectableOptionButton(
            strategies.map { method ->
                OptionSpec(mergeMethodLabel(method)) { vm.mergePullRequest(method, deleteBranchCheckBox.isSelected) }
            },
        )

        val panel = HorizontalListPanel(COMPACT_BUTTONS_GAP).apply {
            add(optionButton)
            add(deleteBranchCheckBox)
        }
        return panel to optionButton
    }

    private class OptionSpec(val label: String, val onConfirm: () -> Unit)

    /**
     * Builds a [JBOptionButton] where clicking a dropdown item only swaps the button's own
     * default [javax.swing.Action] (via [JButton.setAction], which re-renders the label for free)
     * instead of firing — only clicking the primary button itself invokes [OptionSpec.onConfirm].
     * [JBOptionButton] has no built-in "select without firing" mode; this construction pattern is
     * the mechanism (confirmed against the platform sources: it's a plain `JButton(action)` with a
     * separate `options: Array<Action>` for the dropdown — nothing more). All [specs] (including
     * whichever ends up default) go in the dropdown, since re-selecting the current default is a
     * harmless no-op and the default can change at runtime.
     */
    private fun createSelectableOptionButton(specs: List<OptionSpec>): JBOptionButton {
        lateinit var optionButton: JBOptionButton
        fun confirmAction(spec: OptionSpec) = object : AbstractAction(spec.label) {
            override fun actionPerformed(e: ActionEvent?) = spec.onConfirm()
        }
        fun selectAction(spec: OptionSpec) = object : AbstractAction(spec.label) {
            override fun actionPerformed(e: ActionEvent?) {
                optionButton.action = confirmAction(spec)
            }
        }
        optionButton = JBOptionButton(confirmAction(specs.first()), specs.map { selectAction(it) }.toTypedArray())
        return optionButton
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

    /** Spaces a MigLayout cell by [insets] (already scaled). The platform's own `CC.gap(Insets)` is internal API. */
    private fun CC.gaps(insets: java.awt.Insets): CC =
        gapTop("${insets.top}").gapLeft("${insets.left}").gapBottom("${insets.bottom}").gapRight("${insets.right}")
}
