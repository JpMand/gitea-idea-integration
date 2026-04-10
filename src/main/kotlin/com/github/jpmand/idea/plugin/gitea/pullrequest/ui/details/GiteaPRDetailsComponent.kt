@file:Suppress("UnstableApiUsage")

package com.github.jpmand.idea.plugin.gitea.pullrequest.ui.details

import com.github.jpmand.idea.plugin.gitea.api.models.GiteaChangedFile
import com.github.jpmand.idea.plugin.gitea.api.models.GiteaFileStatus
import com.github.jpmand.idea.plugin.gitea.api.models.GiteaPullRequest
import com.github.jpmand.idea.plugin.gitea.pullrequest.file.GiteaPRDiffVirtualFile
import com.github.jpmand.idea.plugin.gitea.pullrequest.file.GiteaPRVirtualFileSystem
import com.github.jpmand.idea.plugin.gitea.pullrequest.service.GiteaPullRequestDataLoader
import com.github.jpmand.idea.plugin.gitea.pullrequest.service.GiteaPullRequestsProjectService
import com.github.jpmand.idea.plugin.gitea.util.GiteaBundle
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.asContextElement
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.ui.SimpleColoredComponent
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTabbedPane
import com.intellij.util.ui.JBUI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.awt.BorderLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.DefaultListModel
import javax.swing.JPanel
import javax.swing.JTextArea
import javax.swing.ListSelectionModel

/**
 * Details panel for a single pull request.
 * Shows PR metadata (title, description, branch info, author) and tabs for
 * Files Changed and Commits.
 *
 * Double-clicking a file in the Files Changed tab opens a [GiteaPRDiffVirtualFile]
 * in the editor via [FileEditorManager].
 */
class GiteaPRDetailsComponent(
    private val project: Project,
    cs: CoroutineScope,
    private val prService: GiteaPullRequestsProjectService,
    private val loader: GiteaPullRequestDataLoader,
    onBack: () -> Unit,
) : JPanel(BorderLayout()) {

    private val filesModel = DefaultListModel<GiteaChangedFile>()
    private val filesList = JBList(filesModel).apply {
        cellRenderer = SimpleListCellRenderer.create("") { file ->
            val prefix = when (file.status) {
                GiteaFileStatus.ADDED -> "[A] "
                GiteaFileStatus.DELETED -> "[D] "
                GiteaFileStatus.MODIFIED -> "[M] "
                GiteaFileStatus.RENAMED -> "[R] "
                else -> "[?] "
            }
            "$prefix${file.filename}"
        }
        selectionMode = ListSelectionModel.SINGLE_SELECTION
        addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (e.clickCount >= 2) {
                    val file = selectedValue ?: return
                    openDiff(file)
                }
            }
        })
    }

    private val descriptionArea = JTextArea().apply {
        isEditable = false
        lineWrap = true
        wrapStyleWord = true
        background = JBUI.CurrentTheme.CustomFrameDecorations.paneBackground()
    }

    private val headerLabel = JBLabel("").apply {
        border = JBUI.Borders.empty(8)
    }

    private val branchLabel = JBLabel("").apply {
        border = JBUI.Borders.empty(0, 8, 4, 8)
    }

    init {
        // Back button / header area
        val topPanel = JPanel(BorderLayout()).apply {
            add(headerLabel, BorderLayout.CENTER)
            add(branchLabel, BorderLayout.SOUTH)
        }
        add(topPanel, BorderLayout.NORTH)

        // Tabs
        val tabs = JBTabbedPane().apply {
            addTab(
                GiteaBundle.message("pullrequest.details.tab.files"),
                JBScrollPane(filesList)
            )
            addTab(
                GiteaBundle.message("pullrequest.details.tab.description"),
                JBScrollPane(descriptionArea)
            )
        }
        add(tabs, BorderLayout.CENTER)

        // Observe PR state
        cs.launch(Dispatchers.EDT + ModalityState.any().asContextElement()) {
            loader.prState.collect { result ->
                val pr = result?.getOrNull() ?: return@collect
                renderPrInfo(pr)
            }
        }

        // Observe files state
        cs.launch(Dispatchers.EDT + ModalityState.any().asContextElement()) {
            loader.filesState.collect { result ->
                val files = result?.getOrNull() ?: return@collect
                filesModel.clear()
                files.forEach { filesModel.addElement(it) }
            }
        }
    }

    private fun renderPrInfo(pr: GiteaPullRequest) {
        headerLabel.text = "#${pr.number} ${pr.title}"
        branchLabel.text = GiteaBundle.message("pullrequest.details.branch.info", pr.headBranch, pr.baseBranch)
        descriptionArea.text = pr.body ?: ""
    }

    private fun openDiff(file: GiteaChangedFile) {
        val pr = loader.prState.value?.getOrNull() ?: return
        val mapping = prService.activeRepoMappingState.value ?: return
        val connectionId = mapping.repository.serverPath.toString()

        val vf = GiteaPRVirtualFileSystem.getInstance()
            .findOrCreateFile(
                project,
                GiteaPRVirtualFileSystem.FilePath(
                    sessionId = connectionId,
                    projectHash = project.locationHash,
                    prNumber = pr.number,
                    baseSha = pr.baseSha,
                    headSha = pr.headSha,
                    relativePath = file.filename,
                )
            ) ?: return

        FileEditorManager.getInstance(project).openFile(vf, true)
    }
}
