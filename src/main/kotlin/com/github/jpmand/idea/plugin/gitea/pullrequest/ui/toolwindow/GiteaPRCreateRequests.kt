package com.github.jpmand.idea.plugin.gitea.pullrequest.ui.toolwindow

import com.intellij.openapi.components.Service
import git4idea.GitBranch
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Bridges "Create Pull Request" from the Git menu, the branches popup and the PR list's toolbar over
 * to [GiteaPRToolWindowController], which owns the "New Pull Request" tab — the same bridging as
 * [GiteaPRCommitSelectionRequests].
 */
@Service(Service.Level.PROJECT)
class GiteaPRCreateRequests {

    /** [head] is the branch to open the pull request from; null for the current branch. */
    data class Request(val head: GitBranch?)

    private val _requests = MutableSharedFlow<Request>(extraBufferCapacity = 1)
    val requests: SharedFlow<Request> = _requests.asSharedFlow()

    fun request(head: GitBranch?) {
        _requests.tryEmit(Request(head))
    }
}
