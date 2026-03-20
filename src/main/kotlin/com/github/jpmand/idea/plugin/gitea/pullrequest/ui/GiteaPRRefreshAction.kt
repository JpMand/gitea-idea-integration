package com.github.jpmand.idea.plugin.gitea.pullrequest.ui

import com.github.jpmand.idea.plugin.gitea.pullrequest.service.GiteaPullRequestsProjectService
import com.github.jpmand.idea.plugin.gitea.pullrequest.ui.list.GiteaPRListViewModel
import com.github.jpmand.idea.plugin.gitea.util.GiteaBundle
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbAwareAction

/**
 * Refreshes the pull-request list for the active repository.
 *
 * Can be instantiated programmatically (with a [GiteaPRListViewModel]) for toolbar use,
 * or registered in `plugin.xml` (where it resolves the service directly).
 */
class GiteaPRRefreshAction(private val vm: GiteaPRListViewModel? = null) :
    DumbAwareAction(
        GiteaBundle.messagePointer("pullrequest.list.action.refresh"),
        AllIcons.Actions.Refresh
    ) {

    override fun actionPerformed(e: AnActionEvent) {
        if (vm != null) {
            vm.refresh()
        } else {
            e.project?.service<GiteaPullRequestsProjectService>()?.refresh()
        }
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
}
