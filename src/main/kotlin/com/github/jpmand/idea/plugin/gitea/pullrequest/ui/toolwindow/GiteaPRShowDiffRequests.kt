package com.github.jpmand.idea.plugin.gitea.pullrequest.ui.toolwindow

import com.github.jpmand.idea.plugin.gitea.api.models.GiteaPullRequest
import com.github.jpmand.idea.plugin.gitea.api.models.GiteaReviewThread
import com.github.jpmand.idea.plugin.gitea.pullrequest.data.GiteaPRDataContext
import com.github.jpmand.idea.plugin.gitea.pullrequest.data.GiteaPRRepository
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
 * thread's diff hunk opens the PR diff at the commented line, not the local file.
 */
@Service(Service.Level.PROJECT)
@Suppress("UnstableApiUsage")
class GiteaPRShowDiffRequests {

    data class Request(
        val pr: GiteaPullRequest,
        val repository: GiteaPRRepository,
        val ctx: GiteaPRDataContext,
        /** The thread's file; for a comment on the old side of a renamed file, its old name. */
        val path: String,
        val scrollRequest: DiffViewerScrollRequest?,
    )

    private val _requests = MutableSharedFlow<Request>(extraBufferCapacity = 1)
    val requests: SharedFlow<Request> = _requests.asSharedFlow()

    fun request(pr: GiteaPullRequest, repository: GiteaPRRepository, ctx: GiteaPRDataContext, thread: GiteaReviewThread) {
        val path = thread.path ?: return
        _requests.tryEmit(Request(pr, repository, ctx, path, thread.diffScrollRequest()))
    }
}

/**
 * Where the PR diff scrolls for this thread: its line, on the side it was made on. None for an
 * outdated thread, whose line numbers belong to an older version of the file than the PR diff
 * shows, nor for a file-level comment.
 */
@Suppress("UnstableApiUsage")
internal fun GiteaReviewThread.diffScrollRequest(): DiffViewerScrollRequest? {
    if (isOutdated) return null
    val location = newLine?.let { Side.RIGHT to it - 1 } ?: oldLine?.let { Side.LEFT to it - 1 } ?: return null
    return DiffViewerScrollRequest.toLine(location)
}
