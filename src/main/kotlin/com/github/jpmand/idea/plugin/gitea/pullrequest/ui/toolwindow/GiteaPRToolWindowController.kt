package com.github.jpmand.idea.plugin.gitea.pullrequest.ui.toolwindow

import com.github.jpmand.idea.plugin.gitea.GiteaRepositoriesManager
import com.github.jpmand.idea.plugin.gitea.api.models.GiteaPullRequest
import com.github.jpmand.idea.plugin.gitea.api.models.GiteaUser
import com.github.jpmand.idea.plugin.gitea.authentication.account.GiteaAccountManager
import com.github.jpmand.idea.plugin.gitea.data.GiteaImageLoader
import com.github.jpmand.idea.plugin.gitea.exception.GiteaHttpStatusErrorAction
import com.github.jpmand.idea.plugin.gitea.pullrequest.GiteaPRTimelineVirtualFile
import com.github.jpmand.idea.plugin.gitea.pullrequest.data.GiteaPRDataContext
import com.github.jpmand.idea.plugin.gitea.pullrequest.data.GiteaPRDataContextHolder
import com.github.jpmand.idea.plugin.gitea.pullrequest.data.GiteaPRRepository
import com.github.jpmand.idea.plugin.gitea.pullrequest.ui.create.GiteaPRCreateComponentFactory
import com.github.jpmand.idea.plugin.gitea.pullrequest.ui.create.GiteaPRCreateViewModel
import com.github.jpmand.idea.plugin.gitea.pullrequest.ui.list.GiteaPRListPanel
import com.github.jpmand.idea.plugin.gitea.pullrequest.ui.list.GiteaPRListViewModel
import com.github.jpmand.idea.plugin.gitea.ui.GiteaSettingsConfigurable
import com.github.jpmand.idea.plugin.gitea.util.GiteaBundle
import com.github.jpmand.idea.plugin.gitea.util.GiteaPluginProjectScopeProvider
import com.intellij.collaboration.ui.icon.AsyncImageIconsProvider
import com.intellij.collaboration.ui.icon.CachingIconsProvider
import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.components.JBPanelWithEmptyText
import com.intellij.ui.content.Content
import com.intellij.ui.content.ContentManagerEvent
import com.intellij.ui.content.ContentManagerListener
import com.intellij.util.ui.UIUtil
import git4idea.GitBranch
import kotlinx.coroutines.*
import javax.swing.JComponent

private val LOG = logger<GiteaPRToolWindowController>()

/**
 * Manages the "Gitea PR" tool window as a tab container:
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

    // Child of the plugin's project scope, tied to this controller's disposable (itself registered
    // on toolWindow.disposable): cancelled when the tool window closes and on plugin unload.
    private val cs = project.service<GiteaPluginProjectScopeProvider>()
        .childScope(javaClass.name, this, Dispatchers.Main.immediate)
    private val cm get() = toolWindow.contentManager

    private var currentCtx: GiteaPRDataContext? = null
    private var listContent: Content? = null
    private var listPanelJob: Job? = null

    private class DetailTab(val content: Content, val scope: CoroutineScope, val tab: GiteaPRDetailsTab)

    private var currentRepository: GiteaPRRepository? = null
    private var currentListVm: GiteaPRListViewModel? = null

    private class CreateTab(val content: Content, val vm: GiteaPRCreateViewModel)

    private var createTab: CreateTab? = null

    private val detailTabs = LinkedHashMap<Int, DetailTab>()

    /**
     * The "+" in the tool window's header. Declared before `init`: the context collector runs on
     * Main.immediate, so the list tab (which sets it) is built inside the constructor.
     */
    private val newPullRequestAction = object : DumbAwareAction(
        GiteaBundle.messagePointer("pull.request.create.tab"),
        GiteaBundle.messagePointer("pull.request.create.action.description"),
        AllIcons.General.Add,
    ) {
        override fun actionPerformed(e: AnActionEvent) = openCreateTab(null)
    }

    init {
        cm.addContentManagerListener(object : ContentManagerListener {
            override fun contentRemoveQuery(event: ContentManagerEvent) {
                if (event.content === listContent) event.consume()
            }

            override fun contentRemoved(event: ContentManagerEvent) {
                if (createTab?.content === event.content) createTab = null
                val entry = detailTabs.entries.firstOrNull { it.value.content === event.content } ?: return
                LOG.debug("Closed the details tab of PR #${entry.key}")
                detailTabs.remove(entry.key)
                entry.value.scope.cancel()
            }
        })
        cs.launch {
            project.service<GiteaPRDataContextHolder>().context.collect { ctx -> updateContent(ctx) }
        }
        cs.launch {
            project.service<GiteaPRCommitSelectionRequests>().requests.collect { req -> handleCommitSelection(req) }
        }
        cs.launch {
            project.service<GiteaPRShowDiffRequests>().requests.collect { req -> handleShowDiff(req) }
        }
        cs.launch {
            project.service<GiteaPRCreateRequests>().requests.collect { req -> openCreateTab(req.head) }
        }
    }

    private fun updateContent(ctx: GiteaPRDataContext?) {
        when {
            ctx == null -> {
                closeAllDetailTabs()
                closeCreateTab()
                closeAllTimelineEditors()
                showEmptyState()
            }
            // GiteaPRDataContextHolder keeps the same context unless the account, repository or
            // token changed, so anything new needs rebuilding: open tabs hold the old API client.
            ctx === currentCtx -> Unit
            else -> {
                closeAllDetailTabs()
                closeCreateTab()
                closeAllTimelineEditors()
                rebuildListTab(ctx)
            }
        }
    }

    private fun showEmptyState() {
        LOG.debug("No PR context, showing the empty state")
        currentCtx = null
        toolWindow.setTitleActions(emptyList())
        listPanelJob?.cancel()
        listPanelJob = null
        val empty = cm.factory.createContent(createEmptyStatePanel(), null, false).apply { isCloseable = false }
        replaceListContent(empty)
    }

    private fun rebuildListTab(ctx: GiteaPRDataContext) {
        LOG.debug("Building the PR list for ${ctx.repo} as ${ctx.account.name}")
        listPanelJob?.cancel()
        val job = SupervisorJob(cs.coroutineContext[Job])
        listPanelJob = job
        val panelCs = CoroutineScope(cs.coroutineContext + job)

        val repository = GiteaPRRepository(ctx)
        val listVm = GiteaPRListViewModel(panelCs, repository)
        currentRepository = repository
        currentListVm = listVm
        val avatarIconsProvider =
            CachingIconsProvider(AsyncImageIconsProvider<GiteaUser>(panelCs, GiteaImageLoader(ctx.api, ctx.avatarImages)))
        val listPanel = GiteaPRListPanel(
            panelCs, listVm, avatarIconsProvider,
            repositoryName = ctx.repo.repositoryPath.toString(),
            repositoryWebUrl = ctx.repo.getWebURI().toString(),
            onPROpenRequested = { pr -> openPullRequest(ctx, repository, pr) },
            onCreateRequested = { openCreateTab(null) },
            // In the controller's scope: saving the new token rebuilds this list, cancelling panelCs.
            logInAgain = GiteaHttpStatusErrorAction.LogInAgain(project, cs, ctx.account, service<GiteaAccountManager>()),
        ).create()

        val content = cm.factory.createContent(
            listPanel,
            ctx.repo.repositoryPath.repository,
            false,
        ).apply {
            isCloseable = false
        }
        replaceListContent(content)
        currentCtx = ctx
        toolWindow.setTitleActions(listOf(newPullRequestAction))
    }

    private fun replaceListContent(content: Content) {
        val old = listContent
        listContent = content
        cm.addContent(content, 0)
        if (old != null) cm.removeContent(old, true)
        cm.setSelectedContent(content)
    }

    private fun openOrFocusDetailTab(ctx: GiteaPRDataContext, repository: GiteaPRRepository, pr: GiteaPullRequest): GiteaPRDetailsTab {
        val number = pr.number.toInt()
        detailTabs[number]?.let {
            LOG.debug("Focusing the details tab of PR #$number")
            cm.setSelectedContent(it.content, true)
            return it.tab
        }

        val tabJob = SupervisorJob(cs.coroutineContext[Job])
        val tabScope = CoroutineScope(cs.coroutineContext + tabJob)
        val tab = GiteaPRDetailsTab(
            project, tabScope, repository, pr,
            onShowTimeline = { openTimelineEditor(repository, pr, ctx) },
        )
        val content = cm.factory.createContent(tab.component, "#${pr.number}", false).apply {
            description = "${ctx.repo.repositoryPath}: ${pr.title}"
            isCloseable = true
            isPinnable = false
            setDisposer(Disposable { tabJob.cancel() })
        }
        detailTabs[number] = DetailTab(content, tabScope, tab)
        cm.addContent(content)
        cm.setSelectedContent(content, true)
        return tab
    }

    /**
     * Opens the "New Pull Request" tab, or focuses it and switches its head to [head] when given. It
     * creates the pull request on the PR context's repository and remote; after that, the new pull
     * request opens like one picked from the list.
     */
    private fun openCreateTab(head: GitBranch?) {
        val ctx = currentCtx ?: return
        val repository = currentRepository ?: return
        toolWindow.activate(null)
        createTab?.let { tab ->
            if (head != null) tab.vm.setHeadBranch(head)
            cm.setSelectedContent(tab.content, true)
            return
        }
        val mapping = project.service<GiteaRepositoriesManager>().knownRepositoriesState.value.firstOrNull {
            it.repository.repositoryPath == ctx.repo.repositoryPath && it.repository.serverPath.equals(ctx.repo.serverPath, ignoreProtocol = true)
        }
        if (mapping == null) {
            LOG.warn("No git repository for ${ctx.repo}, can't create a pull request")
            return
        }
        LOG.info("Opening the New Pull Request tab for ${ctx.repo} (head ${head?.name ?: "current branch"})")
        val tabJob = SupervisorJob(cs.coroutineContext[Job])
        val tabScope = CoroutineScope(cs.coroutineContext + tabJob)
        val vm = GiteaPRCreateViewModel(project, tabScope, repository, mapping, head) { pr ->
            closeCreateTab()
            currentListVm?.refresh()
            openPullRequest(ctx, repository, pr)
        }
        val component = GiteaPRCreateComponentFactory.create(
            tabScope, vm,
            onOpenPullRequest = { pr -> openPullRequest(ctx, repository, pr) },
            onCancel = ::closeCreateTab,
        )
        val content = cm.factory.createContent(component, GiteaBundle.message("pull.request.create.tab"), false).apply {
            description = GiteaBundle.message("pull.request.create.tab.description", ctx.repo.repositoryPath.toString())
            isCloseable = true
            isPinnable = false
            setDisposer(Disposable { tabJob.cancel() })
        }
        createTab = CreateTab(content, vm)
        cm.addContent(content)
        cm.setSelectedContent(content, true)
    }

    private fun closeCreateTab() {
        val tab = createTab ?: return
        createTab = null
        cm.removeContent(tab.content, true)
    }

    /** Opens a PR from the list: both the Details tab and the Conversation timeline editor, not
     * just Details — most users want to start reading/commenting right away. */
    private fun openPullRequest(ctx: GiteaPRDataContext, repository: GiteaPRRepository, pr: GiteaPullRequest) {
        LOG.info("Opening PR #${pr.number} of ${ctx.repo}")
        openOrFocusDetailTab(ctx, repository, pr)
        openTimelineEditor(repository, pr, ctx)
    }

    /** Opens (or focuses) the given PR's Details tab and selects the referenced commit in its
     * changes tree — see [GiteaPRCommitSelectionRequests]. */
    private fun handleCommitSelection(req: GiteaPRCommitSelectionRequests.Request) {
        LOG.debug("Showing commit ${req.commitSha} of PR #${req.pr.number}")
        toolWindow.activate(null)
        val tab = openOrFocusDetailTab(req.ctx, req.repository, req.pr)
        tab.selectCommitBySha(req.commitSha)
    }

    /** Opens the given PR's diff on a review thread's file, through its Details tab (opened if
     * needed), which owns the diff — see [GiteaPRShowDiffRequests]. */
    private fun handleShowDiff(req: GiteaPRShowDiffRequests.Request) {
        LOG.debug("Showing ${req.thread.path} in the diff of PR #${req.pr.number}")
        openOrFocusDetailTab(req.ctx, req.repository, req.pr).showThreadDiff(req.thread)
    }

    private fun openTimelineEditor(repository: GiteaPRRepository, pr: GiteaPullRequest, ctx: GiteaPRDataContext) {
        val file = GiteaPRTimelineVirtualFile(pr.number.toInt(), pr, repository, ctx, project)
        val fileEditorManager = FileEditorManager.getInstance(project)
        // Same-PR safety net for the (rare) case a stale tab survives outside a tracked context
        // change — the actual sweep on every account/repo switch is closeAllTimelineEditors(),
        // called from updateContent(). GiteaPRTimelineVirtualFile.equals() folds in the context
        // (account/repo), so a stale tab never equals the fresh `file` above.
        fileEditorManager.openFiles
            .filterIsInstance<GiteaPRTimelineVirtualFile>()
            .filter { it.prNumber == file.prNumber && it != file }
            .forEach { fileEditorManager.closeFile(it) }
        fileEditorManager.openFile(file, true)
    }

    private fun closeAllDetailTabs() {
        detailTabs.values.toList().forEach { cm.removeContent(it.content, true) }
        detailTabs.clear()
    }

    /**
     * Closes every open Conversation editor tab for this project, regardless of which PR it's
     * for — called whenever the account/repo context changes (or is lost). Mirrors the bundled
     * GitHub plugin's `GHPRFilesManagerImpl.closeAllFiles()` on disconnect: a tab left open from
     * before the switch is built against the old context (including its avatar loader,
     * constructed once from the old `ctx.api`), and would otherwise only get closed lazily, if
     * and when that same PR's timeline happens to be reopened.
     */
    private fun closeAllTimelineEditors() {
        val fileEditorManager = FileEditorManager.getInstance(project)
        fileEditorManager.openFiles
            .filterIsInstance<GiteaPRTimelineVirtualFile>()
            .forEach { fileEditorManager.closeFile(it) }
    }

    /** The platform's centred empty text on the list background, as the other review tool windows show it. */
    private fun createEmptyStatePanel(): JComponent =
        JBPanelWithEmptyText().apply {
            background = UIUtil.getListBackground()
            emptyText
                .appendText(GiteaBundle.message("pull.request.toolwindow.empty.login.title"))
                .appendSecondaryText(GiteaBundle.message("pull.request.toolwindow.empty.login.action"), SimpleTextAttributes.LINK_PLAIN_ATTRIBUTES) {
                    ShowSettingsUtil.getInstance().showSettingsDialog(project, GiteaSettingsConfigurable::class.java)
                }
        }

    override fun dispose() {
        // cs is a disposed scope bound to this Disposable and is cancelled automatically.
    }
}
