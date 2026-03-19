package com.github.jpmand.idea.plugin.gitea.ui.toolwindow

import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Factory for creating the Gitea Pull Requests tool window
 */
class GiteaPullRequestsToolWindowFactory : ToolWindowFactory, DumbAware {

  override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
    val cs = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val panel = GiteaPullRequestsPanel(project, cs)

    val contentFactory = ContentFactory.getInstance()
    val content = contentFactory.createContent(panel, "Pull Requests", false)
    toolWindow.contentManager.addContent(content)

    // Dispose the panel when the tool window is closed
    content.setDisposer {
      panel.dispose()
    }
  }

  override fun shouldBeAvailable(project: Project): Boolean = true
}
