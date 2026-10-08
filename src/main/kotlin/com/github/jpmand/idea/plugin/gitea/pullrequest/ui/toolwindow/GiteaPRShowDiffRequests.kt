package com.github.jpmand.idea.plugin.gitea.pullrequest.ui.toolwindow

import com.github.jpmand.idea.plugin.gitea.api.models.GiteaPullRequest
import com.github.jpmand.idea.plugin.gitea.api.models.GiteaReviewThread
import com.github.jpmand.idea.plugin.gitea.pullrequest.data.GiteaPRDataContext
import com.github.jpmand.idea.plugin.gitea.pullrequest.data.GiteaPRRepository
import com.github.jpmand.idea.plugin.gitea.pullrequest.diff.GiteaPRDiffTarget
import com.intellij.collaboration.ui.codereview.diff.model.DiffViewerScrollRequest
import com.intellij.diff.util.Side
import com.intellij.openapi.components.Service
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Bridges "show this review thread's file in the diff" clicks in the Timeline over to
 * [GiteaPRToolWindowController], whose Details tab owns the PR diff — the same bridging as
 * [GiteaPRCommitSelectionRequests]. As in the GitHub and GitLab plugins, the file name above a
 * thread's diff hunk opens the PR diff at the commented line, not the local file — the diff of the
 * commit the thread's review was made on (see [com.github.jpmand.idea.plugin.gitea.pullrequest.diff.reviewThreadDiffTarget]).
 */
@Service(Service.Level.PROJECT)
class GiteaPRShowDiffRequests {

    data class Request(
        val pr: GiteaPullRequest,
        val repository: GiteaPRRepository,
        val ctx: GiteaPRDataContext,
        /** The thread to show; it has a [GiteaReviewThread.path]. */
        val thread: GiteaReviewThread,
    )

    private val _requests = MutableSharedFlow<Request>(extraBufferCapacity = 1)
    val requests: SharedFlow<Request> = _requests.asSharedFlow()

    fun request(pr: GiteaPullRequest, repository: GiteaPRRepository, ctx: GiteaPRDataContext, thread: GiteaReviewThread) {
        if (thread.path == null) return
        _requests.tryEmit(Request(pr, repository, ctx, thread))
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
