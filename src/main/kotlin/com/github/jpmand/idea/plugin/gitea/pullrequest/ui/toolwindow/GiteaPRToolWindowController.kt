package com.github.jpmand.idea.plugin.gitea.pullrequest.ui.toolwindow

import com.github.jpmand.idea.plugin.gitea.api.models.GiteaPullRequest
import com.github.jpmand.idea.plugin.gitea.api.models.GiteaUser
import com.github.jpmand.idea.plugin.gitea.data.GiteaImageLoader
import com.github.jpmand.idea.plugin.gitea.pullrequest.GiteaPRDetailsVirtualFile
import com.github.jpmand.idea.plugin.gitea.pullrequest.data.GiteaPRDataContext
import com.github.jpmand.idea.plugin.gitea.pullrequest.data.GiteaPRDataContextHolder
import com.github.jpmand.idea.plugin.gitea.pullrequest.data.GiteaPRRepository
import com.github.jpmand.idea.plugin.gitea.pullrequest.ui.list.GiteaPRListPanel
import com.github.jpmand.idea.plugin.gitea.pullrequest.ui.list.GiteaPRListViewModel
import com.github.jpmand.idea.plugin.gitea.ui.GiteaSettingsConfigurable
import com.github.jpmand.idea.plugin.gitea.util.GiteaBundle
import com.intellij.collaboration.ui.icon.AsyncImageIconsProvider
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileEditorManager
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
 * Shows an empty-state panel when no Gitea account/repo is resolved, and the PR list otherwise —
 * matching the GitHub plugin, the tool window shows *only* the list. Opening a PR (double-click
 * or Enter, see [GiteaPRListPanel]) opens its details as a separate editor tab
 * (`GiteaPRDetailsFileEditor`/`GiteaPRDetailsVirtualFile`), not an in-tool-window swap.
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
            createListPanel(ctx, panelCs)
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

    private fun createListPanel(ctx: GiteaPRDataContext, panelCs: CoroutineScope): JComponent {
        val repository = GiteaPRRepository(ctx)
        val listVm = GiteaPRListViewModel(panelCs, repository)
        val avatarIconsProvider = AsyncImageIconsProvider<GiteaUser>(panelCs, GiteaImageLoader(ctx.api))
        val listPanel = GiteaPRListPanel(panelCs, listVm, avatarIconsProvider, onPROpenRequested = { pr ->
            openPRDetailsTab(repository, pr)
        })
        return listPanel.create()
    }

    private fun openPRDetailsTab(repository: GiteaPRRepository, pr: GiteaPullRequest) {
        val file = GiteaPRDetailsVirtualFile(pr.number.toInt(), pr, repository, project)
        FileEditorManager.getInstance(project).openFile(file, true)
    }

    override fun dispose() {
        cs.cancel()
    }
}
