package com.github.jpmand.idea.plugin.gitea.pullrequest.ui.toolwindow

import com.github.jpmand.idea.plugin.gitea.GiteaIcons
import com.github.jpmand.idea.plugin.gitea.util.GiteaBundle
import com.intellij.collaboration.async.childScope
import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.components.serviceAsync
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.platform.util.coroutines.childScope
import com.intellij.ui.IconManager
import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.swing.UIManager

/**
 * Factory that creates the Gitea Pull Requests tool window content.
 * The content is driven by [GiteaPRToolWindowViewModel] and rendered by [GiteaPRsToolWindowController].
 * The coroutine scope is tied to the content's [com.intellij.openapi.Disposable] lifecycle.
 */
@Suppress("UnstableApiUsage")
class GiteaPRToolWindowFactory : ToolWindowFactory, DumbAware {

  override fun init(toolWindow: ToolWindow) {
    toolWindow.setStripeShortTitleProvider { GiteaBundle.message("toolwindow.stripe.Pull_Requests.shortName") }
  }

  override suspend fun manage(toolWindow: ToolWindow, toolWindowManager: ToolWindowManager) {
    //TODO manageIconToolbar(toolWindow)
    toolWindow.project.serviceAsync<GiteaPRToolWindowController>().manageIconToolbar(toolWindow)
  }

  override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
    project.service<GiteaPRToolWindowController>().manageContent(toolWindow)
  }

  override fun shouldBeAvailable(project: Project): Boolean = false

  @Service(Service.Level.PROJECT)
  private class GiteaPRToolWindowController(private val project: Project, parentCs : CoroutineScope){
    private val cs = parentCs.childScope(javaClass.name, Dispatchers.Main)

    suspend fun manageContent(toolWindow: ToolWindow) {
      coroutineScope {
        val vm = project.serviceAsync<GiteaPRProjectViewModel>()
        launch{
          vm.isAvailable.collect {
            withContext(Dispatchers.EDT){
              toolWindow.isAvailable = it
            }
          }
        }

        launch{
          vm.activationRequests.collect {
            withContext(Dispatchers.EDT){
              toolWindow.activate(null)
            }
          }
        }

        val focusColor = UIManager.getColor("ToolWindow.Button.selectedForeground")
        launch {
          vm.connectedProjectVm
            .filterNotNull()
            .flatMapLatest { it.listVm.hasUpdates }
            .distinctUntilChanged()
            .collectLatest {
              withContext(Dispatchers.EDT){
                toolWindow.setIcon(
                  if(it == null || !it) GiteaIcons.PullRequestToolWindow
                  else IconManager.getInstance().withIconBadge(GiteaIcons.PullRequestToolWindow, JBColor {
                    if(toolWindow.isActive) focusColor else JBUI.CurrentTheme.IconBadge.INFORMATION
                  }))
              }
            }
        }
      }
    }
  }
}
