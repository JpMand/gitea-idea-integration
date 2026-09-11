package com.github.jpmand.idea.plugin.gitea.pullrequest.ui.details

import com.github.jpmand.idea.plugin.gitea.GiteaRepositoriesManager
import com.github.jpmand.idea.plugin.gitea.api.models.GiteaPullRequest
import com.github.jpmand.idea.plugin.gitea.pullrequest.data.GiteaPRRepository
import com.github.jpmand.idea.plugin.gitea.util.GiteaBundle
import com.github.jpmand.idea.plugin.gitea.util.GiteaGitRepositoryMapping
import com.intellij.collaboration.ui.codereview.details.model.CodeReviewBranches
import com.intellij.collaboration.ui.codereview.details.model.CodeReviewBranchesViewModel
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import git4idea.branch.GitBrancher
import git4idea.fetch.GitFetchSupport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * "Checkout" for the PR-details header: fetches the PR's head commit and checks out a local
 * branch for it, using git4idea's public [GitFetchSupport] / [GitBrancher] — no local git working
 * copy is touched until the user explicitly asks for this.
 */
@Suppress("UnstableApiUsage")
class GiteaPRBranchesViewModel(
    private val project: Project,
    private val cs: CoroutineScope,
    private val repository: GiteaPRRepository,
    private val prFlow: StateFlow<GiteaPullRequest>,
) : CodeReviewBranchesViewModel {

    override val sourceBranch: StateFlow<String> = prFlow
        .map { it.head.ref }
        .stateIn(cs, SharingStarted.Eagerly, prFlow.value.head.ref)

    private val _isCheckedOut = MutableStateFlow(isCurrentlyCheckedOut(prFlow.value))
    override val isCheckedOut: SharedFlow<Boolean> = _isCheckedOut

    private val _showBranchesRequests = MutableSharedFlow<CodeReviewBranches>()
    override val showBranchesRequests: SharedFlow<CodeReviewBranches> = _showBranchesRequests

    override fun fetchAndCheckoutRemoteBranch() {
        val pr = prFlow.value
        val mapping = findRepositoryMapping()
        if (mapping == null) {
            notifyError(GiteaBundle.message("pull.request.branch.checkout.no.repository"))
            return
        }
        val gitRepository = mapping.gitRepository
        val remote = mapping.gitRemote

        // Gitea (like GitHub) maintains `refs/pull/<index>/head` in the base repository, kept in
        // sync with the PR's current head commit — this works uniformly for same-repo and fork
        // PRs alike, so there's no need to add the fork as a separate remote.
        val prRef = "refs/pull/${pr.number}/head"
        val localRef = "refs/gitea/pr/${pr.number}/head"
        val branchName = "gitea/pr-${pr.number}"

        cs.launch {
            val fetchResult = withContext(Dispatchers.IO) {
                GitFetchSupport.fetchSupport(project).fetch(gitRepository, remote, "$prRef:$localRef")
            }
            if (!fetchResult.isSuccessful()) {
                withContext(Dispatchers.EDT) { fetchResult.showNotificationIfFailed() }
                return@launch
            }
            withContext(Dispatchers.EDT) {
                GitBrancher.getInstance(project).checkoutNewBranchStartingFrom(
                    branchName, localRef, listOf(gitRepository),
                ) {
                    _isCheckedOut.value = true
                }
            }
        }
    }

    override fun showBranches() {
        cs.launch {
            val pr = prFlow.value
            _showBranchesRequests.emit(CodeReviewBranches(pr.head.ref, pr.base.ref))
        }
    }

    /**
     * A one-time snapshot taken when the VM is created (or after a checkout this session) —
     * not reactive to branches switched from elsewhere while the details tab stays open.
     * Detects only branches created by [fetchAndCheckoutRemoteBranch]'s own naming convention.
     */
    private fun isCurrentlyCheckedOut(pr: GiteaPullRequest): Boolean {
        val gitRepository = findRepositoryMapping()?.gitRepository ?: return false
        return gitRepository.currentBranch?.name == "gitea/pr-${pr.number}"
    }

    private fun findRepositoryMapping(): GiteaGitRepositoryMapping? {
        val coordinates = repository.repositoryCoordinates
        return project.service<GiteaRepositoriesManager>().knownRepositoriesState.value.firstOrNull { mapping ->
            mapping.repository.repositoryPath == coordinates.repositoryPath &&
                mapping.repository.serverPath.equals(coordinates.serverPath, ignoreProtocol = true)
        }
    }

    private fun notifyError(message: String) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("Gitea")
            .createNotification(GiteaBundle.message("pull.request.branch.checkout.error.title"), message, NotificationType.ERROR)
            .notify(project)
    }
}
