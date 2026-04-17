package com.github.jpmand.idea.plugin.gitea.pullrequest.ui.toolwindow

import com.github.jpmand.idea.plugin.gitea.pullrequest.data.GiteaPRDataContext
import com.github.jpmand.idea.plugin.gitea.pullrequest.data.GiteaPRDataContextHolder
import com.github.jpmand.idea.plugin.gitea.pullrequest.data.GiteaPRRepository
import com.github.jpmand.idea.plugin.gitea.pullrequest.ui.list.GiteaPRListPanel
import com.github.jpmand.idea.plugin.gitea.pullrequest.ui.list.GiteaPRListViewModel
import com.github.jpmand.idea.plugin.gitea.ui.GiteaSettingsConfigurable
import com.github.jpmand.idea.plugin.gitea.util.GiteaBundle
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.service
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.ui.HyperlinkLabel
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.SwingConstants
import javax.swing.event.HyperlinkEvent

/**
 * Manages tool window contents in reaction to the [GiteaPRDataContextHolder] state.
 *
 * Shows an empty-state panel when no Gitea account/repo is resolved, and a PR panel
 * (populated in Phase 4) when a full context is available.
 */
@Suppress("UnstableApiUsage")
class GiteaPRToolWindowController(
    private val project: Project,
    private val toolWindow: ToolWindow,
) : Disposable {

    private val cs = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    init {
        cs.launch {
            project.service<GiteaPRDataContextHolder>().context
                .collect { ctx -> updateContent(ctx) }
        }
    }

    private var currentPanelJob: Job? = null

    private fun updateContent(ctx: GiteaPRDataContext?) {
        currentPanelJob?.cancel()
        currentPanelJob = null
        val cm = toolWindow.contentManager
        cm.removeAllContents(true)
        val panel = if (ctx == null) {
            createEmptyStatePanel()
        } else {
            val panelJob = SupervisorJob(cs.coroutineContext[Job])
            currentPanelJob = panelJob
            val panelCs = CoroutineScope(cs.coroutineContext + panelJob)
            createPRPanel(ctx, panelCs)
        }
        cm.addContent(cm.factory.createContent(panel, null, false))
    }

    private fun createEmptyStatePanel(): JComponent {
        val titleLabel = JBLabel(GiteaBundle.message("pull.request.toolwindow.empty.login.title")).apply {
            foreground = UIUtil.getContextHelpForeground()
            horizontalAlignment = SwingConstants.CENTER
        }
        val settingsLink = HyperlinkLabel(GiteaBundle.message("pull.request.toolwindow.empty.login.action")).apply {
            addHyperlinkListener { e ->
                if (e.eventType == HyperlinkEvent.EventType.ACTIVATED) {
                    ShowSettingsUtil.getInstance().showSettingsDialog(project, GiteaSettingsConfigurable::class.java)
                }
            }
        }
        return JPanel(GridBagLayout()).apply {
            val c = GridBagConstraints()
            c.gridx = 0; c.gridy = 0; c.insets = JBUI.insetsBottom(UIUtil.DEFAULT_VGAP)
            add(titleLabel, c)
            c.gridy = 1; c.insets = JBUI.emptyInsets()
            add(settingsLink, c)
        }
    }

    private fun createPRPanel(ctx: GiteaPRDataContext, panelCs: CoroutineScope): JComponent {
        val repository = GiteaPRRepository(ctx)
        val vm = GiteaPRListViewModel(panelCs, repository)
        return GiteaPRListPanel(panelCs, vm).create()
    }

    override fun dispose() {
        cs.cancel()
    }
}
