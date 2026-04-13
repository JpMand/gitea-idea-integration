package com.github.jpmand.idea.plugin.gitea

import com.github.jpmand.idea.plugin.gitea.ui.GiteaSettingsConfigurable
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.currentOrDefaultProject

class GiteaOpenSettingsAction : DumbAwareAction() {
  override fun actionPerformed(e: AnActionEvent) {
    val project = currentOrDefaultProject(e.project)
    ShowSettingsUtil.getInstance().showSettingsDialog(project, GiteaSettingsConfigurable::class.java)
  }

  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
}