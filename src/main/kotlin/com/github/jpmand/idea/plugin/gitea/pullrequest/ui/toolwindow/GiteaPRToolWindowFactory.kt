package com.github.jpmand.idea.plugin.gitea.pullrequest.ui.toolwindow

import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory

class GiteaPRToolWindowFactory : ToolWindowFactory, DumbAware {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val controller = GiteaPRToolWindowController(project, toolWindow)
        Disposer.register(toolWindow.disposable, controller)
    }

    override fun shouldBeAvailable(project: Project): Boolean = true
}
