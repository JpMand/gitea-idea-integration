package com.github.jpmand.idea.plugin.gitea.pullrequest.ui.toolwindow

import com.github.jpmand.idea.plugin.gitea.util.GiteaPluginProjectScopeProvider
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.asContextElement
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import kotlinx.coroutines.Dispatchers

/**
 * Factory that creates the Gitea Pull Requests tool window content.
 * The content is driven by [GiteaPRToolWindowViewModel] and rendered by [GiteaPRToolWindowController].
 * The coroutine scope is tied to the content's [com.intellij.openapi.Disposable] lifecycle.
 */
@Suppress("UnstableApiUsage")
class GiteaPRToolWindowFactory : ToolWindowFactory, DumbAware {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val scopeProvider = project.service<GiteaPluginProjectScopeProvider>()
        val contentManager = toolWindow.contentManager

        val content = contentManager.factory.createContent(null, null, false)

        val scope = scopeProvider.createDisposedScope(
            GiteaPRToolWindowFactory::class.java.name,
            content,
            Dispatchers.EDT + ModalityState.any().asContextElement()
        )

        val vm = GiteaPRToolWindowViewModel(project, scope)
        val controller = GiteaPRToolWindowController(project, scope, vm)

        content.component = controller
        content.isCloseable = false
        contentManager.addContent(content)
    }

    override fun shouldBeAvailable(project: Project): Boolean = true
}
