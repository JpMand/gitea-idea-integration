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
import com.intellij.collaboration.ui.CollaborationToolsUIUtil
import com.intellij.collaboration.ui.codereview.diff.model.DiffViewerScrollRequest
import com.intellij.openapi.application.EDT
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JPanel

private val LOG = logger<GiteaPRDetailsTab>()

/**
 * Builds the read-only PR-details content hosted as a closeable tool-window tab (`#<number>`).
 * Owns nothing that needs explicit disposal beyond [cs], which the controller cancels when the
 * tab closes.
 */
@Suppress("UnstableApiUsage")
class GiteaPRDetailsTab(
    private val project: Project,
    private val cs: CoroutineScope,
    repository: GiteaPRRepository,
    pr: GiteaPullRequest,
    onShowTimeline: () -> Unit,
) {

    private val detailsVm = GiteaPRDetailsViewModel(project, cs, pr, repository)
    private val statusVm = GiteaPRStatusViewModel(cs, detailsVm.prFlow, detailsVm.branchesVm.localMergeabilityState, repository)
    // The diff, the review threads and the changes tree follow the refreshed PR (detailsVm.prFlow),
    // so a push shows up in all of them, not just in the header and commit list.
    private val diffVm = GiteaPRDiffViewModel(cs, project, detailsVm.prFlow, repository)
    private val discussionsVm = GiteaPRDiscussionsViewModels(project, cs, pr.number.toInt(), pr.head.sha, repository, pr.mentionCandidates(), pr.author.login)
        .also { vm -> cs.launch { detailsVm.prFlow.collect { vm.updateHeadSha(it.head.sha) } } }
    private val diffFile = GiteaPRDiffVirtualFile(pr.number.toInt(), cs, project, repository, diffVm, discussionsVm)

    private val changesComponent = GiteaPRChangesTreeComponentFactory.create(
        cs, project, detailsVm.prFlow, repository, discussionsVm,
        selectedCommitFlow = detailsVm.changesVm.selectedCommit,
        onOpenChange = { relPath -> showDiff(relPath, null) },
    )

    private val refresh: () -> Unit = {
        repository.dropSharedLoads()
        detailsVm.refresh()
        discussionsVm.reload()
    }

    /**
     * Opens the PR diff on [path] — a file's current name or, for a renamed file, its old one —
     * scrolled by [scrollRequest], once the PR's changed files have loaded. Does nothing for a
     * file that isn't part of the PR diff (any more).
     */
    fun showDiff(path: String, scrollRequest: DiffViewerScrollRequest?) {
        cs.launch {
            val files = diffVm.changes.first { it?.result != null }?.result?.getOrNull()?.selectedChanges?.list.orEmpty()
            val idx = files.indexOfFirst { it.file.filename == path || it.file.previousFilename == path }
            if (idx < 0) {
                LOG.debug("$path is not among the ${files.size} changed files of the PR diff")
                return@launch
            }
            withContext(Dispatchers.EDT) {
                diffVm.showChange(idx, scrollRequest)
                FileEditorManager.getInstance(project).openFile(diffFile, true)
            }
        }
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
        val details = GiteaPRDetailsPanel(
            project, cs, detailsVm, statusVm, discussionsVm, changesComponent,
            onShowTimeline = onShowTimeline,
            onRefresh = refresh,
        ).create()
        // The platform progress stripe while the PR is re-fetched (on open and on Refresh).
        add(CollaborationToolsUIUtil.wrapWithProgressStripe(cs, detailsVm.isLoading, details), BorderLayout.CENTER)
    }
}
