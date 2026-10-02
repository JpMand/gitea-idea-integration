package com.github.jpmand.idea.plugin.gitea.pullrequest.ui.action

import com.github.jpmand.idea.plugin.gitea.pullrequest.data.GiteaPRDataContextHolder
import com.github.jpmand.idea.plugin.gitea.pullrequest.ui.toolwindow.GiteaPRCreateRequests
import com.github.jpmand.idea.plugin.gitea.util.GiteaBundle
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import git4idea.GitBranch
import git4idea.GitLocalBranch
import git4idea.actions.branch.GitSingleBranchAction
import git4idea.repo.GitRepository

/** Git ▸ Create Gitea Pull Request…: opens the "New Pull Request" tab for the current branch. */
class GiteaCreatePullRequestAction : DumbAwareAction() {
    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val project = e.project
        e.presentation.isEnabledAndVisible = project != null && project.service<GiteaPRDataContextHolder>().context.value != null
    }

    override fun actionPerformed(e: AnActionEvent) {
        e.project?.service<GiteaPRCreateRequests>()?.request(null)
    }
}

/** "Create Gitea Pull Request…" on a local branch in the branches popup: that branch as the head. */
class GiteaCreatePullRequestFromBranchAction : GitSingleBranchAction(GiteaBundle.messagePointer("pull.request.create.branch.action")) {

    override val disabledForRemote: Boolean = true

    override fun updateIfEnabledAndVisible(e: AnActionEvent, project: Project, repositories: List<GitRepository>, reference: GitBranch) {
        val ctx = project.service<GiteaPRDataContextHolder>().context.value
        e.presentation.isEnabledAndVisible = ctx != null && reference is GitLocalBranch
    }

    override fun actionPerformed(e: AnActionEvent, project: Project, repositories: List<GitRepository>, reference: GitBranch) {
        project.service<GiteaPRCreateRequests>().request(reference)
    }
}
