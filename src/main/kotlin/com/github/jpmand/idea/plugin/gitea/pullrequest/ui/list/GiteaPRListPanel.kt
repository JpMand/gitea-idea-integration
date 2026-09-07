package com.github.jpmand.idea.plugin.gitea.pullrequest.ui.list

import com.github.jpmand.idea.plugin.gitea.api.models.GiteaPullRequest
import com.github.jpmand.idea.plugin.gitea.api.models.GiteaUser
import com.github.jpmand.idea.plugin.gitea.pullrequest.ui.GiteaPRActionKeys
import com.github.jpmand.idea.plugin.gitea.pullrequest.ui.action.GiteaPROpenPullRequestAction
import com.github.jpmand.idea.plugin.gitea.pullrequest.ui.filters.GiteaPRListSearchPanelFactory
import com.github.jpmand.idea.plugin.gitea.util.GiteaBundle
import com.intellij.collaboration.ui.codereview.avatar.Avatar
import com.intellij.collaboration.ui.codereview.list.NamedCollection
import com.intellij.collaboration.ui.codereview.list.ReviewListComponentFactory
import com.intellij.collaboration.ui.codereview.list.ReviewListItemPresentation
import com.intellij.collaboration.ui.codereview.list.ReviewListUtil
import com.intellij.collaboration.ui.codereview.list.TagPresentation
import com.intellij.collaboration.ui.codereview.list.UserPresentation
import com.intellij.collaboration.ui.icon.IconsProvider
import com.intellij.openapi.actionSystem.CommonShortcuts
import com.intellij.openapi.actionSystem.CompositeShortcutSet
import com.intellij.openapi.actionSystem.DataSink
import com.intellij.openapi.actionSystem.UiDataProvider
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.ui.ColorHexUtil
import icons.CollaborationToolsIcons
import kotlinx.coroutines.CoroutineScope
import java.awt.BorderLayout
import java.awt.Color
import javax.swing.JComponent
import javax.swing.JPanel

/**
 * Renders the PR list only — no in-place details view (that's now a separate editor tab, see
 * `GiteaPRDetailsFileEditor`). A row is only ever *selected* by a single click or arrow-key
 * navigation; opening a PR requires double-click or Enter, matching the GitHub plugin's
 * action-system-based interaction (not a plain `ListSelectionListener`).
 */
@Suppress("UnstableApiUsage")
class GiteaPRListPanel(
    private val cs: CoroutineScope,
    private val vm: GiteaPRListViewModel,
    private val avatarIconsProvider: IconsProvider<GiteaUser>,
    private val onPROpenRequested: (GiteaPullRequest) -> Unit,
) {

    fun create(): JComponent {
        val l = ReviewListComponentFactory(vm.listModel).create { pr ->
            createPresentation(pr)
        }

        val openAction = GiteaPROpenPullRequestAction(onPROpenRequested)
        val openShortcuts = CompositeShortcutSet(CommonShortcuts.ENTER, CommonShortcuts.DOUBLE_CLICK_1)
        ActionUtil.wrap(openAction).registerCustomShortcutSet(openShortcuts, l)

        val searchPanel = GiteaPRListSearchPanelFactory(vm.searchVm).create(cs)
        // wrapWithLazyVerticalScroll requires the raw JList<?> (it hooks scroll listeners onto
        // it directly), so the list can't itself be replaced by wrapComponent's wrapper here.
        // wrapComponent returns a NEW JComponent wrapping its argument rather than mutating it
        // in place, so the data-provider capability has to be attached to the scroll pane
        // (still an ancestor of `l` once added below) instead — DataContext resolution walks up
        // from whichever component receives the click/Enter, so this is equivalent for the
        // action system while satisfying wrapWithLazyVerticalScroll's type requirement.
        val scrollPane = ReviewListUtil.wrapWithLazyVerticalScroll(cs, l) { /* pagination deferred */ }
        val scrollPaneWithData = UiDataProvider.wrapComponent(scrollPane) { sink: DataSink ->
            l.selectedValue?.let { pr -> sink[GiteaPRActionKeys.SELECTED_PULL_REQUEST] = pr }
        }

        return JPanel(BorderLayout()).apply {
            add(searchPanel, BorderLayout.NORTH)
            add(scrollPaneWithData, BorderLayout.CENTER)
        }
    }

    private fun createPresentation(pr: GiteaPullRequest): ReviewListItemPresentation {
        val stateText: String? = when {
            pr.draft -> GiteaBundle.message("pull.request.state.draft")
            pr.merged -> GiteaBundle.message("pull.request.state.merged")
            pr.state == "closed" -> GiteaBundle.message("pull.request.state.closed")
            else -> null
        }

        val author = UserPresentation.Simple(
            username = pr.author.login,
            fullName = pr.author.fullName,
            avatarIcon = avatarIconsProvider.getIcon(pr.author, Avatar.Sizes.BASE),
        )

        val tags = pr.labels.map { label ->
            TagPresentation.Simple(label.name, parseColor(label.color))
        }

        val assignees = pr.assignees.map { user ->
            UserPresentation.Simple(user.login, user.fullName, avatarIconsProvider.getIcon(user, Avatar.Sizes.BASE))
        }

        // Loaded lazily, one REST call per PR the first time its row is rendered (not for the
        // whole page eagerly) — returns null (no group shown yet) until the fetch resolves,
        // at which point this row is re-rendered automatically. See GiteaPRListViewModel.
        val reviewers = vm.reviewsFor(pr.number)?.let { reviews ->
            sortedReviewerStates(computeReviewerStates(pr.requestedReviewers, reviews)).map { (user, _) ->
                UserPresentation.Simple(user.login, user.fullName, avatarIconsProvider.getIcon(user, Avatar.Sizes.OUTLINED))
            }
        } ?: emptyList()

        // Gitea only exposes a boolean "mergeable" flag (no GitHub-style tri-state); shown only
        // for plain open, non-draft PRs to avoid false positives (Gitea often reports
        // mergeable=false on drafts before it has computed anything meaningful).
        val mergeableStatus = if (!pr.mergeable && pr.state == "open" && !pr.merged && !pr.draft) {
            ReviewListItemPresentation.Status(
                CollaborationToolsIcons.Review.NonMergeable,
                GiteaBundle.message("pull.request.not.mergeable.tooltip"),
            )
        } else null

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
            mergeableStatus = mergeableStatus,
            state = stateText,
            userGroup1 = NamedCollection.create(
                GiteaBundle.message("pull.request.assignees.popup", assignees.size), assignees
            ),
            userGroup2 = NamedCollection.create(
                GiteaBundle.message("pull.request.reviewers.popup", reviewers.size), reviewers
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
