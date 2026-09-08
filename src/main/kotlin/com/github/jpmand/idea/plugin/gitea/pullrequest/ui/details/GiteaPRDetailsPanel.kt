package com.github.jpmand.idea.plugin.gitea.pullrequest.ui.details

import com.github.jpmand.idea.plugin.gitea.util.GiteaBundle
import com.intellij.collaboration.ui.SimpleHtmlPane
import com.intellij.collaboration.ui.VerticalListPanel
import com.intellij.collaboration.ui.codereview.details.CodeReviewDetailsStatusComponentFactory
import com.intellij.collaboration.ui.codereview.details.CodeReviewDetailsTitleComponentFactory
import com.intellij.collaboration.ui.util.emptyBorders
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.components.ActionLink
import com.intellij.util.ui.JBUI
import kotlinx.coroutines.CoroutineScope
import net.miginfocom.layout.LC
import net.miginfocom.swing.MigLayout
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.ScrollPaneConstants

/**
 * Read-only PR-details tool-window tab: title + `#number` + status, plus the changed-files tree.
 * Branches/commits/description moved to the activity-timeline editor (opened via [onShowTimeline]).
 */
@Suppress("UnstableApiUsage")
class GiteaPRDetailsPanel(
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

        val navBar = JPanel(MigLayout(LC().emptyBorders().fillX().noGrid())).apply {
            isOpaque = false
            add(ActionLink(GiteaBundle.message("pull.request.action.show.timeline")) { onShowTimeline() }
                .apply { border = JBUI.Borders.empty(4, 8) })
            add(ActionLink(GiteaBundle.message("pull.request.action.refresh")) { onRefresh() }
                .apply { border = JBUI.Borders.empty(4, 8) })
        }

        val statusComponent = VerticalListPanel(4).apply {
            add(CodeReviewDetailsStatusComponentFactory.createCiComponent(cs, statusVm))
            add(CodeReviewDetailsStatusComponentFactory.createNeedReviewerComponent(cs, statusVm.reviewerStates))
            add(CodeReviewDetailsStatusComponentFactory.createConflictsComponent(cs, statusVm.hasConflicts))
        }

        val header = VerticalListPanel(8).apply {
            border = JBUI.Borders.empty(8)
            add(titleComponent)
            add(statusComponent)
        }
        val headerScroll = ScrollPaneFactory.createScrollPane(header, true).apply {
            horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
        }

        return JPanel(MigLayout(LC().emptyBorders().fill().flowY().noGrid().gridGap("0", "0"))).apply {
            isOpaque = false
            add(navBar, net.miginfocom.layout.CC().growX())
            add(headerScroll, net.miginfocom.layout.CC().growX())
            add(changesComponent, net.miginfocom.layout.CC().grow().push())
        }
    }

    private fun createActionGroup(): ActionGroup = DefaultActionGroup().apply {
        add(object : AnAction(GiteaBundle.message("pull.request.action.open.in.browser")) {
            override fun actionPerformed(e: AnActionEvent) {
                BrowserUtil.browse(vm.url)
            }
        })
    }
}
