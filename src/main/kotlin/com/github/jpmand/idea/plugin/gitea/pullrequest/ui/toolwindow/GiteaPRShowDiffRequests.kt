package com.github.jpmand.idea.plugin.gitea.pullrequest.ui.toolwindow

import com.github.jpmand.idea.plugin.gitea.api.models.GiteaPullRequest
import com.github.jpmand.idea.plugin.gitea.api.models.GiteaReviewThread
import com.github.jpmand.idea.plugin.gitea.pullrequest.data.GiteaPRDataContext
import com.github.jpmand.idea.plugin.gitea.pullrequest.data.GiteaPRRepository
import com.github.jpmand.idea.plugin.gitea.pullrequest.diff.GiteaPRDiffTarget
import com.intellij.collaboration.ui.codereview.diff.model.DiffViewerScrollRequest
import com.intellij.diff.util.Side
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.util.concurrency.annotations.RequiresEdt
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Bridges "show this in the PR diff" clicks from outside the tool window over to
 * [GiteaPRToolWindowController], whose Details tab owns the PR diff — the same bridging as
 * [GiteaPRCommitSelectionRequests]. As in the GitHub and GitLab plugins:
 *  - the file name above a review thread's diff hunk in the Timeline opens the PR diff at the
 *    commented line, not the local file — the diff of the commit the thread's review was made on
 *    (see [com.github.jpmand.idea.plugin.gitea.pullrequest.diff.reviewThreadDiffTarget]);
 *  - "Show Diff" on a PR change marker in a regular editor opens the PR diff of that file, at
 *    that line.
 */
@Service(Service.Level.PROJECT)
class GiteaPRShowDiffRequests(private val project: Project) {

    data class Request(
        val pr: GiteaPullRequest,
        val repository: GiteaPRRepository,
        val ctx: GiteaPRDataContext,
        val location: Location,
    )

    sealed interface Location {
        /** A review thread's file; the thread has a [GiteaReviewThread.path]. */
        data class Thread(val thread: GiteaReviewThread) : Location

        /** [path] in the PR diff, scrolled to the 0-based head line [headLine] if there is one. */
        data class File(val path: String, val headLine: Int?) : Location
    }

    private val _requests = MutableSharedFlow<Request>(extraBufferCapacity = 1)
    val requests: SharedFlow<Request> = _requests.asSharedFlow()

    fun request(pr: GiteaPullRequest, repository: GiteaPRRepository, ctx: GiteaPRDataContext, thread: GiteaReviewThread) {
        if (thread.path == null) return
        emit(Request(pr, repository, ctx, Location.Thread(thread)))
    }

    fun requestFile(pr: GiteaPullRequest, repository: GiteaPRRepository, ctx: GiteaPRDataContext, path: String, headLine: Int?) =
        emit(Request(pr, repository, ctx, Location.File(path, headLine)))

    /**
     * Only a [GiteaPRToolWindowController] takes requests, and the platform creates it with the
     * tool window's content, the first time the tool window is shown. A regular editor can ask
     * before that, so the content is created first — without showing the tool window, as the
     * GitHub plugin opens its PR diff from the editor without it.
     */
    @RequiresEdt
    private fun emit(request: Request) {
        if (_requests.subscriptionCount.value == 0) {
            // Reading the content manager creates the content (and so the controller) if needed.
            ToolWindowManager.getInstance(project).getToolWindow(GiteaPRToolWindowFactory.ID)?.contentManager
        }
        if (_requests.subscriptionCount.value == 0) {
            LOG.warn("No Gitea PR tool window to show the diff of PR #${request.pr.number} in")
            return
        }
        _requests.tryEmit(request)
    }

    companion object {
        private val LOG = logger<GiteaPRShowDiffRequests>()
    }
}

/**
 * Where the diff of [target] scrolls for this thread: its line, on the side it was made on. None
 * for a file-level comment, nor for an outdated thread in the PR diff, whose line numbers belong
 * to an older version of the file than the PR diff shows — any other target shows the file at the
 * commit the thread's review was made on, where its line numbers hold.
 */
@Suppress("UnstableApiUsage")
internal fun GiteaReviewThread.diffScrollRequest(target: GiteaPRDiffTarget = GiteaPRDiffTarget.PullRequest): DiffViewerScrollRequest? {
    if (isOutdated && target == GiteaPRDiffTarget.PullRequest) return null
    val location = newLine?.let { Side.RIGHT to it - 1 } ?: oldLine?.let { Side.LEFT to it - 1 } ?: return null
    return DiffViewerScrollRequest.toLine(location)
}
