package com.github.jpmand.idea.plugin.gitea.pullrequest.ui.toolwindow

import com.github.jpmand.idea.plugin.gitea.api.models.GiteaPullRequest
import com.github.jpmand.idea.plugin.gitea.api.models.mentionCandidates
import com.github.jpmand.idea.plugin.gitea.pullrequest.data.GiteaPRRepository
import com.github.jpmand.idea.plugin.gitea.pullrequest.diff.GiteaPRDiffViewModel
import com.github.jpmand.idea.plugin.gitea.pullrequest.diff.GiteaPRDiffVirtualFile
import com.github.jpmand.idea.plugin.gitea.pullrequest.review.GiteaPRDiscussionsViewModels
import com.github.jpmand.idea.plugin.gitea.pullrequest.ui.details.GiteaPRChangesTreeComponentFactory
import com.github.jpmand.idea.plugin.gitea.pullrequest.ui.details.GiteaPRDetailsPanel
import com.github.jpmand.idea.plugin.gitea.pullrequest.ui.details.GiteaPRDetailsViewModel
import com.github.jpmand.idea.plugin.gitea.pullrequest.ui.details.GiteaPRStatusViewModel
import com.github.jpmand.idea.plugin.gitea.pullrequest.ui.giteaReviewErrorPanel
import com.github.jpmand.idea.plugin.gitea.util.GiteaBundle
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CoroutineScope
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JPanel

/**
 * Builds the read-only PR-details content hosted as a closeable tool-window tab (`#<number>`).
 * Owns nothing that needs explicit disposal beyond [cs], which the controller cancels when the
 * tab closes.
 */
@Suppress("UnstableApiUsage")
class GiteaPRDetailsTab(
    private val project: Project,
    cs: CoroutineScope,
    repository: GiteaPRRepository,
    pr: GiteaPullRequest,
    onShowTimeline: () -> Unit,
) {

    private val detailsVm = GiteaPRDetailsViewModel(project, cs, pr, repository)
    private val statusVm = GiteaPRStatusViewModel(cs, pr, repository)
    private val diffVm = GiteaPRDiffViewModel(cs, project, pr, repository)
    private val discussionsVm = GiteaPRDiscussionsViewModels(project, cs, pr.number.toInt(), pr.head.sha, repository, pr.mentionCandidates())
    private val diffFile = GiteaPRDiffVirtualFile(pr.number.toInt(), cs, project, repository, diffVm, discussionsVm)

    private val changesComponent = GiteaPRChangesTreeComponentFactory.create(
        cs, project, pr, repository, discussionsVm,
        selectedCommitFlow = detailsVm.changesVm.selectedCommit,
        onOpenChange = { relPath ->
            val list = diffVm.changes.value?.result?.getOrNull()?.selectedChanges?.list.orEmpty()
            val idx = list.indexOfFirst { it.file.filename == relPath }
            if (idx >= 0) {
                diffVm.showChange(idx, null)
                FileEditorManager.getInstance(project).openFile(diffFile, true)
            }
        },
    )

    private val refresh: () -> Unit = {
        detailsVm.refresh()
        discussionsVm.reload()
    }

    /** Selects the given commit in the changes tree — used when a "referenced/added commit" is
     * clicked from the Timeline (see [GiteaPRCommitSelectionRequests]). */
    fun selectCommitBySha(sha: String) = detailsVm.changesVm.selectCommitBySha(sha)

    val component: JComponent = JPanel(BorderLayout()).apply {
        add(
            giteaReviewErrorPanel(
                cs, detailsVm.error, GiteaBundle.message("pull.request.details.load.error"), onRetry = refresh,
            ),
            BorderLayout.NORTH,
        )
        add(
            GiteaPRDetailsPanel(
                project, cs, detailsVm, statusVm, discussionsVm, changesComponent,
                onShowTimeline = onShowTimeline,
                onRefresh = refresh,
            ).create(),
            BorderLayout.CENTER,
        )
    }
}
