package com.github.jpmand.idea.plugin.gitea.pullrequest.ui.toolwindow

import com.github.jpmand.idea.plugin.gitea.pullrequest.data.GiteaPRDataContextHolder
import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.openapi.wm.impl.content.ToolWindowContentUi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * The "Gitea Pull Requests" tool window. It is available only while
 * [GiteaPRDataContextHolder.context] is set, that is while an account with a stored token is on
 * the same server as one of the project's git remotes. Without one there is nothing to show, and
 * every other pull request feature (the in-editor review, the PR diff, the Conversation tab)
 * hangs off that same context, so they go away with it.
 */
class GiteaPRToolWindowFactory : ToolWindowFactory, DumbAware {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        // The tabs name the repository and PRs; the "Gitea Pull Requests:" prefix only takes room.
        toolWindow.component.putClientProperty(ToolWindowContentUi.HIDE_ID_LABEL, "true")
        val controller = GiteaPRToolWindowController(project, toolWindow)
        Disposer.register(toolWindow.disposable, controller)
    }

    /** Hidden until [manage] finds a usable account and remote. */
    override fun shouldBeAvailable(project: Project): Boolean = false

    /** Shows and hides the tool window as accounts, tokens and git remotes change. */
    override suspend fun manage(toolWindow: ToolWindow, toolWindowManager: ToolWindowManager) {
        toolWindow.project.service<GiteaPRDataContextHolder>().context
            .map { it != null }
            .distinctUntilChanged()
            .collect { available ->
                withContext(Dispatchers.EDT) { toolWindow.isAvailable = available }
            }
    }
}
