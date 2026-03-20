package com.github.jpmand.idea.plugin.gitea.pullrequest.ui

import com.github.jpmand.idea.plugin.gitea.pullrequest.service.GiteaPullRequestsProjectService
import com.github.jpmand.idea.plugin.gitea.util.GiteaBundle
import com.intellij.icons.AllIcons
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbAwareAction

/**
 * Opens the active repository's pull-request list in the default browser.
 */
class GiteaPROpenOnWebAction : DumbAwareAction(
    GiteaBundle.messagePointer("pullrequest.details.open.on.web"),
    AllIcons.General.Web
) {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val prService = project.service<GiteaPullRequestsProjectService>()
        val mapping = prService.activeRepoMappingState.value ?: return
        val baseUri = mapping.repository.getWebURI().toString().trimEnd('/')
        BrowserUtil.browse("$baseUri/pulls")
    }

    override fun update(e: AnActionEvent) {
        val project = e.project ?: run { e.presentation.isEnabled = false; return }
        val prService = project.service<GiteaPullRequestsProjectService>()
        e.presentation.isEnabled = prService.activeRepoMappingState.value != null
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
}
