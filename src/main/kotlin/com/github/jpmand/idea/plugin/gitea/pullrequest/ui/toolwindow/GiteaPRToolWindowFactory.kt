package com.github.jpmand.idea.plugin.gitea.pullrequest.ui.toolwindow

import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.openapi.wm.impl.content.ToolWindowContentUi

class GiteaPRToolWindowFactory : ToolWindowFactory, DumbAware {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        // The tabs name the repository and PRs; the "Gitea Pull Requests:" prefix only takes room.
        toolWindow.component.putClientProperty(ToolWindowContentUi.HIDE_ID_LABEL, "true")
        val controller = GiteaPRToolWindowController(project, toolWindow)
        Disposer.register(toolWindow.disposable, controller)
    }

    override fun shouldBeAvailable(project: Project): Boolean = true
}
