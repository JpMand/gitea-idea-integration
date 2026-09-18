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
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import git4idea.branch.GitBrancher
import git4idea.fetch.GitFetchSupport
import git4idea.remote.hosting.GitCodeReviewUtils
import git4idea.repo.GitRemote
import git4idea.repo.GitRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.coroutines.resume

/**
 * "Checkout" for the PR-details header: fetches the PR's head commit (Gitea, like GitHub,
 * maintains `refs/pull/<index>/head` in the base repository, kept in sync with the PR's current
 * head commit — this works uniformly for same-repo and fork PRs alike, so there's no need to add
 * the fork as a separate remote) and checks out a local branch for it, using
 * [GitCodeReviewUtils.fetch] (the same review-ref fetch helper the bundled GitHub/GitLab plugins
 * use) and git4idea's public [GitBrancher] — no local git working copy is touched until the user
 * explicitly asks for this.
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

    private val _isResolvingConflicts = MutableStateFlow(false)
    val isResolvingConflicts: StateFlow<Boolean> = _isResolvingConflicts.asStateFlow()

    override fun fetchAndCheckoutRemoteBranch() {
        val mapping = findRepositoryMapping()
        if (mapping == null) {
            notifyError(GiteaBundle.message("pull.request.branch.checkout.no.repository"))
            return
        }
        cs.launch { checkoutPrBranch(mapping, prFlow.value) }
    }

    override fun showBranches() {
        cs.launch {
            val pr = prFlow.value
            _showBranchesRequests.emit(CodeReviewBranches(pr.head.ref, pr.base.ref))
        }
    }

    /**
     * Checks out the PR branch locally (if not already), fetches the base branch, then merges the
     * base branch's remote-tracking ref into it via [GitBrancher.merge]. On conflicts, git4idea's
     * own merge machinery surfaces its native "Conflicts" dialog automatically — no plugin-side
     * dialog code needed. Resolution and any follow-up commit/push stay manual.
     */
    fun resolveConflicts() {
        val mapping = findRepositoryMapping()
        if (mapping == null) {
            notifyError(GiteaBundle.message("pull.request.branch.checkout.no.repository"))
            return
        }
        val pr = prFlow.value

        cs.launch {
            _isResolvingConflicts.value = true
            try {
                if (!_isCheckedOut.value && !checkoutPrBranch(mapping, pr)) return@launch

                val baseFetch = withContext(Dispatchers.IO) {
                    GitFetchSupport.fetchSupport(project).fetch(mapping.gitRepository, mapping.gitRemote)
                }
                if (!baseFetch.isSuccessful()) {
                    withContext(Dispatchers.EDT) { baseFetch.showNotificationIfFailed() }
                    return@launch
                }

                withContext(Dispatchers.EDT) {
                    GitBrancher.getInstance(project).merge(
                        "${mapping.gitRemote.name}/${pr.base.ref}",
                        GitBrancher.DeleteOnMergeOption.NOTHING,
                        listOf(mapping.gitRepository),
                    )
                }
            } finally {
                withContext(NonCancellable) { _isResolvingConflicts.value = false }
            }
        }
    }

    private suspend fun checkoutPrBranch(mapping: GiteaGitRepositoryMapping, pr: GiteaPullRequest): Boolean {
        val prRef = "refs/pull/${pr.number}/head"
        val localRef = "refs/${pr.head.ref}/head"
        val branchName = pr.head.ref

        if (!fetch(mapping.gitRepository, mapping.gitRemote, "$prRef:$localRef")) return false

        return suspendCancellableCoroutine { cont ->
            ApplicationManager.getApplication().invokeLater {
                GitBrancher.getInstance(project).checkoutNewBranchStartingFrom(
                    branchName, localRef, listOf(mapping.gitRepository),
                ) {
                    _isCheckedOut.value = true
                    cont.resume(true)
                }
            }
        }
    }

    /**
     * Fetches [refspec] via [GitCodeReviewUtils.fetch] — unlike [GitFetchSupport] (still used for
     * the base-branch fetch in [resolveConflicts], which has no refspec to hand it), it only
     * throws on failure rather than returning a result to check, so failures are turned into a
     * plugin notification here instead.
     */
    private suspend fun fetch(gitRepository: GitRepository, remote: GitRemote, refspec: String): Boolean =
        try {
            GitCodeReviewUtils.fetch(gitRepository, remote, refspec)
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val detail = e.message?.let { ":\n$it" } ?: ""
            notifyError(GiteaBundle.message("pull.request.branch.checkout.fetch.failed", detail))
            false
        }

    /**
     * A one-time snapshot taken when the VM is created (or after a checkout this session) —
     * not reactive to branches switched from elsewhere while the details tab stays open.
     * Detects only branches created by [fetchAndCheckoutRemoteBranch]'s own naming convention.
     */
    private fun isCurrentlyCheckedOut(pr: GiteaPullRequest): Boolean {
        val gitRepository = findRepositoryMapping()?.gitRepository ?: return false
        return gitRepository.currentBranch?.name == pr.head.ref
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
