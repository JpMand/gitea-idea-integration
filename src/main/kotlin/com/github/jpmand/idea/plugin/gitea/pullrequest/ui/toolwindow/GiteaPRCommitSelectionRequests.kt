package com.github.jpmand.idea.plugin.gitea.pullrequest.ui.toolwindow

import com.github.jpmand.idea.plugin.gitea.api.models.GiteaPullRequest
import com.github.jpmand.idea.plugin.gitea.pullrequest.data.GiteaPRDataContext
import com.github.jpmand.idea.plugin.gitea.pullrequest.data.GiteaPRRepository
import com.intellij.openapi.components.Service
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Bridges "open this commit" clicks in the Timeline (a [com.intellij.openapi.fileEditor.FileEditor]
 * tab, with no direct reference to the ToolWindow) over to
 * [GiteaPRToolWindowController][com.github.jpmand.idea.plugin.gitea.pullrequest.ui.toolwindow.GiteaPRToolWindowController]
 * (which owns the Details tab / changes tree) — same flow-based bridging pattern as
 * [com.github.jpmand.idea.plugin.gitea.pullrequest.data.GiteaPRDataContextHolder].
 */
@Service(Service.Level.PROJECT)
class GiteaPRCommitSelectionRequests {

    data class Request(
        val pr: GiteaPullRequest,
        val repository: GiteaPRRepository,
        val ctx: GiteaPRDataContext,
        val commitSha: String,
    )

    private val _requests = MutableSharedFlow<Request>(extraBufferCapacity = 1)
    val requests: SharedFlow<Request> = _requests.asSharedFlow()

    fun request(pr: GiteaPullRequest, repository: GiteaPRRepository, ctx: GiteaPRDataContext, commitSha: String) {
        _requests.tryEmit(Request(pr, repository, ctx, commitSha))
    }
}
