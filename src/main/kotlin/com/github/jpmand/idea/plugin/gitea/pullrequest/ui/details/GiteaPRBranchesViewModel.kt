package com.github.jpmand.idea.plugin.gitea.pullrequest.ui.details

import com.github.jpmand.idea.plugin.gitea.GiteaRepositoriesManager
import com.github.jpmand.idea.plugin.gitea.api.models.GiteaPullRequest
import com.github.jpmand.idea.plugin.gitea.pullrequest.data.GiteaPRRepository
import com.github.jpmand.idea.plugin.gitea.util.GiteaBundle
import com.github.jpmand.idea.plugin.gitea.util.GiteaGitRepositoryMapping
import com.github.jpmand.idea.plugin.gitea.util.GiteaUtil
import com.intellij.collaboration.ui.codereview.details.model.CodeReviewBranches
import com.intellij.collaboration.ui.codereview.details.model.CodeReviewBranchesViewModel
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageDialogBuilder
import git4idea.GitRevisionNumber
import git4idea.branch.GitBrancher
import git4idea.commands.Git
import git4idea.commands.GitCommand
import git4idea.commands.GitLineHandler
import git4idea.fetch.GitFetchSupport
import git4idea.history.GitHistoryUtils
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

    /**
     * Independent, local-git mergeability check — Gitea's own `mergeable` flag (surfaced via
     * [GiteaPRStatusViewModel.hasConflicts]) is coarse, so this adds a real merge-tree dry run
     * against the *remote* target branch (never the possibly-stale local one — always fetched
     * first). `null` means "unknown" (fetch failed, git too old for the two-arg `merge-tree` form,
     * no repository mapping, ...) and deliberately does NOT add extra gating beyond whatever
     * Gitea's flag already says — only a confirmed local conflict (`false`) does.
     */
    private val _localMergeabilityState = MutableStateFlow<Boolean?>(null)
    val localMergeabilityState: StateFlow<Boolean?> = _localMergeabilityState.asStateFlow()

    init {
        cs.launch(Dispatchers.IO) {
            prFlow.distinctUntilChanged { old, new -> old.head.sha == new.head.sha && old.base.ref == new.base.ref }
                .collectLatest { pr -> _localMergeabilityState.value = computeLocalMergeability(pr) }
        }
    }

    private suspend fun computeLocalMergeability(pr: GiteaPullRequest): Boolean? {
        val mapping = findRepositoryMapping() ?: return null
        return try {
            val baseFetch = withContext(Dispatchers.IO) {
                GitFetchSupport.fetchSupport(project).fetch(mapping.gitRepository, mapping.gitRemote)
            }
            if (!baseFetch.isSuccessful()) return null

            val localRef = "refs/${pr.head.ref}/head"
            if (!fetch(mapping.gitRepository, mapping.gitRemote, "refs/pull/${pr.number}/head:$localRef")) return null

            val root = mapping.gitRepository.root
            val targetRef = "${mapping.gitRemote.name}/${pr.base.ref}"

            val mergeBase = GitHistoryUtils.getMergeBase(project, root, targetRef, localRef) ?: return null
            val targetRevision = GitRevisionNumber.resolve(project, root, targetRef)
            if (mergeBase.asString() == targetRevision.asString()) return true // target is already an ancestor of head

            val handler = GitLineHandler(project, root, GitCommand.MERGE_TREE).apply {
                setSilent(true)
                addParameters(targetRef, localRef)
            }
            Git.getInstance().runCommand(handler).success()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            GiteaUtil.LOG.debug("Local mergeability check failed for PR #${pr.number}", e)
            null
        }
    }

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

    /**
     * If [branchName] already exists locally, this updates it (fetch + merge the PR's new head in,
     * same native-conflict-dialog idiom as [resolveConflicts]) instead of erroring the way a bare
     * [GitBrancher.checkoutNewBranchStartingFrom] would. A mismatched upstream is offered as a
     * fix-it confirmation *before* the fetch (a cheap local check, independent of it) — declining
     * doesn't abort the update, it just leaves the existing tracking info alone.
     */
    private suspend fun checkoutPrBranch(mapping: GiteaGitRepositoryMapping, pr: GiteaPullRequest): Boolean {
        val prRef = "refs/pull/${pr.number}/head"
        val localRef = "refs/${pr.head.ref}/head"
        val branchName = pr.head.ref

        val existingBranch = mapping.gitRepository.branches.findLocalBranch(branchName) != null
        if (existingBranch) fixMismatchedUpstreamIfConfirmed(mapping, pr, branchName)

        if (!fetch(mapping.gitRepository, mapping.gitRemote, "$prRef:$localRef")) return false

        return if (existingBranch) updateExistingBranch(mapping, localRef, branchName) else checkoutNewBranch(mapping, localRef, branchName)
    }

    private suspend fun checkoutNewBranch(mapping: GiteaGitRepositoryMapping, localRef: String, branchName: String): Boolean =
        suspendCancellableCoroutine { cont ->
            ApplicationManager.getApplication().invokeLater {
                GitBrancher.getInstance(project).checkoutNewBranchStartingFrom(
                    branchName, localRef, listOf(mapping.gitRepository),
                ) {
                    _isCheckedOut.value = true
                    cont.resume(true)
                }
            }
        }

    private suspend fun updateExistingBranch(mapping: GiteaGitRepositoryMapping, localRef: String, branchName: String): Boolean {
        if (mapping.gitRepository.currentBranch?.name != branchName) {
            suspendCancellableCoroutine { cont ->
                ApplicationManager.getApplication().invokeLater {
                    GitBrancher.getInstance(project).checkout(branchName, false, listOf(mapping.gitRepository)) {
                        cont.resume(Unit)
                    }
                }
            }
        }
        withContext(Dispatchers.EDT) {
            GitBrancher.getInstance(project).merge(localRef, GitBrancher.DeleteOnMergeOption.NOTHING, listOf(mapping.gitRepository))
        }
        _isCheckedOut.value = true
        return true
    }

    private suspend fun fixMismatchedUpstreamIfConfirmed(mapping: GiteaGitRepositoryMapping, pr: GiteaPullRequest, branchName: String) {
        val trackInfo = mapping.gitRepository.getBranchTrackInfo(branchName)
        val matches = trackInfo != null &&
            trackInfo.remote.name == mapping.gitRemote.name &&
            trackInfo.remoteBranch.nameForRemoteOperations == pr.head.ref
        if (matches) return

        val expectedUpstream = "${mapping.gitRemote.name}/${pr.head.ref}"
        val confirmed = withContext(Dispatchers.EDT) {
            MessageDialogBuilder.yesNo(
                GiteaBundle.message("pull.request.branch.checkout.upstream.mismatch.title"),
                GiteaBundle.message("pull.request.branch.checkout.upstream.mismatch.message", branchName, expectedUpstream),
            ).asWarning().ask(project)
        }
        if (confirmed) {
            withContext(Dispatchers.IO) {
                Git.getInstance().setUpstream(mapping.gitRepository, expectedUpstream, branchName)
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
