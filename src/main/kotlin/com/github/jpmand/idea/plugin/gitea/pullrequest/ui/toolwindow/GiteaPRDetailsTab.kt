package com.github.jpmand.idea.plugin.gitea.pullrequest.ui.toolwindow

import com.github.jpmand.idea.plugin.gitea.api.models.GiteaPullRequest
import com.github.jpmand.idea.plugin.gitea.api.models.GiteaReviewThread
import com.github.jpmand.idea.plugin.gitea.api.models.mentionCandidates
import com.github.jpmand.idea.plugin.gitea.pullrequest.data.GiteaPRRepository
import com.github.jpmand.idea.plugin.gitea.pullrequest.diff.GiteaPRDiffTarget
import com.github.jpmand.idea.plugin.gitea.pullrequest.diff.GiteaPRDiffViewModel
import com.github.jpmand.idea.plugin.gitea.pullrequest.diff.GiteaPRDiffVirtualFile
import com.github.jpmand.idea.plugin.gitea.pullrequest.diff.reviewThreadDiffTarget
import com.github.jpmand.idea.plugin.gitea.pullrequest.review.GiteaPRDiscussionsViewModels
import com.github.jpmand.idea.plugin.gitea.pullrequest.ui.details.GiteaPRChangesTreeComponentFactory
import com.github.jpmand.idea.plugin.gitea.pullrequest.ui.details.GiteaPRDetailsPanel
import com.github.jpmand.idea.plugin.gitea.pullrequest.ui.details.GiteaPRDetailsViewModel
import com.github.jpmand.idea.plugin.gitea.pullrequest.ui.details.GiteaPRStatusViewModel
import com.github.jpmand.idea.plugin.gitea.pullrequest.ui.giteaReviewErrorPanel
import com.github.jpmand.idea.plugin.gitea.util.GiteaBundle
import com.intellij.collaboration.ui.CollaborationToolsUIUtil
import com.intellij.collaboration.ui.codereview.diff.model.DiffViewerScrollRequest
import com.intellij.diff.util.Side
import com.intellij.openapi.application.EDT
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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
    private val repository: GiteaPRRepository,
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
        onOpenChange = { target, relPath -> showDiff(target, relPath, null) },
    )

    private val refresh: () -> Unit = {
        repository.dropSharedLoads()
        detailsVm.refresh()
        discussionsVm.reload()
    }

    /**
     * Opens the PR diff of [target] on [path] — a file's current name or, for a renamed file, its
     * old one — scrolled by [scrollRequest], once that diff's files have loaded. Does nothing for a
     * file that isn't part of that diff (any more).
     */
    fun showDiff(target: GiteaPRDiffTarget, path: String, scrollRequest: DiffViewerScrollRequest?) {
        LOG.debug("Opening $path in the diff of $target")
        cs.launch {
            if (!diffVm.selectFile(target, path, scrollRequest)) return@launch
            withContext(Dispatchers.EDT) {
                FileEditorManager.getInstance(project).openFile(diffFile, true)
            }
        }
    }

    /**
     * Opens the diff the review [thread] belongs in — see [reviewThreadDiffTarget] — on its file,
     * at its line. Falls back to the PR diff if the review's commit can't be looked up.
     */
    fun showThreadDiff(thread: GiteaReviewThread) {
        val path = thread.path ?: return
        cs.launch {
            val target = try {
                reviewThreadTarget(thread, path)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                LOG.warn("Couldn't look up review commit ${thread.reviewCommitId} of thread ${thread.id}, showing the PR diff", e)
                GiteaPRDiffTarget.PullRequest
            }
            LOG.debug("Thread ${thread.id} (review commit ${thread.reviewCommitId}) opens the diff of $target")
            showDiff(target, path, thread.diffScrollRequest(target))
        }
    }

    private suspend fun reviewThreadTarget(thread: GiteaReviewThread, path: String): GiteaPRDiffTarget {
        val sha = thread.reviewCommitId ?: return GiteaPRDiffTarget.PullRequest
        val pr = detailsVm.prFlow.value
        return withContext(Dispatchers.IO) {
            val commit = repository.loadCommits(pr.number.toInt()).firstOrNull { it.sha == sha }
            val commitFiles = if (commit == null) emptyList() else repository.loadCommitChangedFiles(sha)
            val side = if (thread.newLine == null && thread.oldLine != null) Side.LEFT else Side.RIGHT
            reviewThreadDiffTarget(commit, commitFiles, path, side, pr.head.sha, pr.diffBaseSha)
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
