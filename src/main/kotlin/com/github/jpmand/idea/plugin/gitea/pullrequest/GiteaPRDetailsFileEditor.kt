package com.github.jpmand.idea.plugin.gitea.pullrequest

import com.github.jpmand.idea.plugin.gitea.api.models.GiteaPullRequest
import com.github.jpmand.idea.plugin.gitea.pullrequest.data.GiteaPRRepository
import com.github.jpmand.idea.plugin.gitea.pullrequest.diff.GiteaPRDiffVirtualFile
import com.github.jpmand.idea.plugin.gitea.pullrequest.diff.GiteaPRDiffViewModel
import com.github.jpmand.idea.plugin.gitea.pullrequest.review.GiteaPRDiscussionsViewModels
import com.github.jpmand.idea.plugin.gitea.pullrequest.ui.details.GiteaPRDetailsPanel
import com.github.jpmand.idea.plugin.gitea.pullrequest.ui.details.GiteaPRDetailsViewModel
import com.github.jpmand.idea.plugin.gitea.pullrequest.ui.details.GiteaPRStatusViewModel
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.VirtualFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import java.beans.PropertyChangeListener
import javax.swing.JComponent

/**
 * Editor-tab implementation of the PR details view (see [GiteaPRDetailsVirtualFile]) — the
 * GitHub-plugin-aligned replacement for the old in-tool-window details swap. Owns its own
 * coroutine scope and the view models it builds, mirroring what
 * `GiteaPRToolWindowController.showDetails()` used to construct inline.
 */
@Suppress("UnstableApiUsage")
class GiteaPRDetailsFileEditor(
    private val project: Project,
    private val file: GiteaPRDetailsVirtualFile,
    repository: GiteaPRRepository,
    private val pr: GiteaPullRequest,
) : UserDataHolderBase(), FileEditor {

    private val cs = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val detailsVm = GiteaPRDetailsViewModel(cs, pr, repository)
    private val statusVm = GiteaPRStatusViewModel(cs, pr, repository)
    private val diffVm = GiteaPRDiffViewModel(cs, project, pr, repository)
    private val discussionsVm = GiteaPRDiscussionsViewModels(cs, pr.number.toInt(), repository)
    private val diffFile = GiteaPRDiffVirtualFile(pr.number.toInt(), cs, project, diffVm, discussionsVm)

    private val component: JComponent = GiteaPRDetailsPanel(
        cs, detailsVm, statusVm, diffVm,
        onViewChanges = {
            FileEditorManager.getInstance(project).openFile(diffFile, true)
        },
        onRefresh = {
            detailsVm.refresh()
            discussionsVm.reload()
        },
    ).create()

    override fun getComponent(): JComponent = component
    override fun getPreferredFocusedComponent(): JComponent? = null
    override fun getName(): String = "#${pr.number} ${pr.title}"
    override fun setState(state: FileEditorState) {}
    override fun isModified(): Boolean = false
    override fun isValid(): Boolean = !project.isDisposed
    override fun addPropertyChangeListener(listener: PropertyChangeListener) {}
    override fun removePropertyChangeListener(listener: PropertyChangeListener) {}
    override fun getFile(): VirtualFile = file

    override fun dispose() {
        cs.cancel()
    }
}
