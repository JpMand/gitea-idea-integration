package com.github.jpmand.idea.plugin.gitea.pullrequest.ui.toolwindow

import com.github.jpmand.idea.plugin.gitea.api.models.GiteaPullRequest
import com.github.jpmand.idea.plugin.gitea.api.models.GiteaUser
import com.github.jpmand.idea.plugin.gitea.data.GiteaImageLoader
import com.github.jpmand.idea.plugin.gitea.pullrequest.GiteaPRTimelineVirtualFile
import com.github.jpmand.idea.plugin.gitea.pullrequest.data.GiteaPRDataContext
import com.github.jpmand.idea.plugin.gitea.pullrequest.data.GiteaPRDataContextHolder
import com.github.jpmand.idea.plugin.gitea.pullrequest.data.GiteaPRRepository
import com.github.jpmand.idea.plugin.gitea.pullrequest.ui.list.GiteaPRListPanel
import com.github.jpmand.idea.plugin.gitea.pullrequest.ui.list.GiteaPRListViewModel
import com.github.jpmand.idea.plugin.gitea.ui.GiteaSettingsConfigurable
import com.github.jpmand.idea.plugin.gitea.util.GiteaBundle
import com.intellij.collaboration.ui.icon.AsyncImageIconsProvider
import com.intellij.collaboration.ui.icon.CachingIconsProvider
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.ui.HyperlinkLabel
import com.intellij.ui.components.JBLabel
import com.intellij.ui.content.Content
import com.intellij.ui.content.ContentManagerEvent
import com.intellij.ui.content.ContentManagerListener
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
 * Manages the "Gitea Pull Requests" tool window as a tab container:
 *  - a fixed, non-closeable first tab (named after the repository) holding the PR list;
 *  - one closeable tab per opened PR (`#<number>`), holding the read-only details view.
 *
 * The activity timeline is still an editor tab (see [GiteaPRTimelineVirtualFile]), opened from the
 * "Show Conversation" link inside a details tab.
 */
@Suppress("UnstableApiUsage")
class GiteaPRToolWindowController(
    private val project: Project,
    private val toolWindow: ToolWindow,
) : Disposable {

    private val cs = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val cm get() = toolWindow.contentManager

    private var currentCtx: GiteaPRDataContext? = null
    private var listContent: Content? = null
    private var listPanelJob: Job? = null

    private class DetailTab(val content: Content, val scope: CoroutineScope)

    private val detailTabs = LinkedHashMap<Int, DetailTab>()

    init {
        cm.addContentManagerListener(object : ContentManagerListener {
            override fun contentRemoveQuery(event: ContentManagerEvent) {
                if (event.content === listContent) event.consume()
            }

            override fun contentRemoved(event: ContentManagerEvent) {
                val entry = detailTabs.entries.firstOrNull { it.value.content === event.content } ?: return
                detailTabs.remove(entry.key)
                entry.value.scope.cancel()
            }
        })
        cs.launch {
            project.service<GiteaPRDataContextHolder>().context.collect { ctx -> updateContent(ctx) }
        }
    }

    private fun updateContent(ctx: GiteaPRDataContext?) {
        when {
            ctx == null -> {
                closeAllDetailTabs()
                showEmptyState()
            }
            ctx.repo == currentCtx?.repo && ctx.account == currentCtx?.account -> {
                // Same repo/account (e.g. token refresh) — keep the open tabs as they are.
                currentCtx = ctx
            }
            else -> {
                closeAllDetailTabs()
                rebuildListTab(ctx)
            }
        }
    }

    private fun showEmptyState() {
        currentCtx = null
        listPanelJob?.cancel()
        listPanelJob = null
        val empty = cm.factory.createContent(createEmptyStatePanel(), null, false).apply { isCloseable = false }
        replaceListContent(empty)
    }

    private fun rebuildListTab(ctx: GiteaPRDataContext) {
        listPanelJob?.cancel()
        val job = SupervisorJob(cs.coroutineContext[Job])
        listPanelJob = job
        val panelCs = CoroutineScope(cs.coroutineContext + job)

        val repository = GiteaPRRepository(ctx)
        val listVm = GiteaPRListViewModel(panelCs, repository)
        val avatarIconsProvider =
            CachingIconsProvider(AsyncImageIconsProvider<GiteaUser>(panelCs, GiteaImageLoader(ctx.api)))
        val listPanel = GiteaPRListPanel(
            panelCs, listVm, avatarIconsProvider,
            repositoryWebUrl = ctx.repo.getWebURI().toString(),
            onPROpenRequested = { pr -> openOrFocusDetailTab(ctx, repository, pr) },
        ).create()

        val content = cm.factory.createContent(
            listPanel,
            ctx.repo.repositoryPath.repository,
            false,
        ).apply {
            isCloseable = false
            isPinned = true
        }
        replaceListContent(content)
        currentCtx = ctx
    }

    private fun replaceListContent(content: Content) {
        val old = listContent
        listContent = content
        cm.addContent(content, 0)
        if (old != null) cm.removeContent(old, true)
        cm.setSelectedContent(content)
    }

    private fun openOrFocusDetailTab(ctx: GiteaPRDataContext, repository: GiteaPRRepository, pr: GiteaPullRequest) {
        val number = pr.number.toInt()
        detailTabs[number]?.let {
            cm.setSelectedContent(it.content, true)
            return
        }

        val tabJob = SupervisorJob(cs.coroutineContext[Job])
        val tabScope = CoroutineScope(cs.coroutineContext + tabJob)
        val tab = GiteaPRDetailsTab(
            project, tabScope, repository, pr,
            onShowTimeline = { openTimelineEditor(repository, pr, ctx) },
        )
        val content = cm.factory.createContent(tab.component, "#${pr.number}", false).apply {
            isCloseable = true
            isPinnable = false
            setDisposer(Disposable { tabJob.cancel() })
        }
        detailTabs[number] = DetailTab(content, tabScope)
        cm.addContent(content)
        cm.setSelectedContent(content, true)
    }

    private fun openTimelineEditor(repository: GiteaPRRepository, pr: GiteaPullRequest, ctx: GiteaPRDataContext) {
        val file = GiteaPRTimelineVirtualFile(pr.number.toInt(), pr, repository, ctx, project)
        FileEditorManager.getInstance(project).openFile(file, true)
    }

    private fun closeAllDetailTabs() {
        detailTabs.values.toList().forEach { cm.removeContent(it.content, true) }
        detailTabs.clear()
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

    override fun dispose() {
        cs.cancel()
    }
}
