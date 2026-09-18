package com.github.jpmand.idea.plugin.gitea.pullrequest.data

import com.github.jpmand.idea.plugin.gitea.GiteaRepositoriesManager
import com.github.jpmand.idea.plugin.gitea.api.models.GiteaPullRequest
import com.github.jpmand.idea.plugin.gitea.api.models.mentionCandidates
import com.github.jpmand.idea.plugin.gitea.pullrequest.diff.GiteaPRChangedFile
import com.github.jpmand.idea.plugin.gitea.pullrequest.review.GiteaPRDiscussionsViewModels
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import git4idea.remote.hosting.infoFlow
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * One resolved "PR whose head branch is checked out locally right now" — the trigger for Phase
 * 9's in-editor review annotations (see the diff-review milestone's Phase 9 TODO note). Bundles
 * the same [GiteaPRDiscussionsViewModels] the diff tab and Details tab use, so a regular editor
 * and those surfaces share drafts/threads/pending-review state for the same PR.
 */
data class GiteaPRForCurrentBranch(
    val ctx: GiteaPRDataContext,
    val pr: GiteaPullRequest,
    val repository: GiteaPRRepository,
    val discussionsVm: GiteaPRDiscussionsViewModels,
    val changedFiles: List<GiteaPRChangedFile>,
    /** The git4idea repository root this PR's branch is checked out in — used to turn an editor's
     * [VirtualFile] into the repo-relative path [changedFiles] entries are keyed by. */
    val gitRepositoryRoot: VirtualFile,
)

/**
 * Project service that tracks which open PR (if any) has its head branch checked out locally right
 * now, for [GiteaPRDataContextHolder]'s currently active context. Recomputes whenever that context
 * or the matching git4idea repository's current branch changes — branch changes are observed via
 * [git4idea.repo.GitRepository]'s own `GIT_REPO_CHANGE` message-bus topic (through git4idea's
 * [infoFlow] wrapper), the same reactive seam [com.github.jpmand.idea.plugin.gitea.GiteaRepositoriesManagerImpl]
 * uses for remotes.
 *
 * If more than one open PR shares the same head branch (rare — e.g. close/reopen churn), the
 * most-recently-updated one wins.
 */
@Service(Service.Level.PROJECT)
class GiteaPRForCurrentBranchService(private val project: Project, private val cs: CoroutineScope) {

    private val _current = MutableStateFlow<GiteaPRForCurrentBranch?>(null)
    val current: StateFlow<GiteaPRForCurrentBranch?> = _current.asStateFlow()

    /** The [GiteaPRDiscussionsViewModels]/repository pair currently held by [_current] is scoped
     * to this job — cancelled whenever superseded, so its background polling (threads, mentions,
     * pending-review) doesn't outlive the branch/PR it was built for. */
    private var prScopeJob: Job? = null

    init {
        cs.launch {
            project.service<GiteaPRDataContextHolder>().context.collectLatest { ctx ->
                if (ctx == null) {
                    setCurrent(null)
                    return@collectLatest
                }
                val mapping = project.service<GiteaRepositoriesManager>().knownRepositoriesState.value.firstOrNull {
                    it.repository.repositoryPath == ctx.repo.repositoryPath &&
                        it.repository.serverPath.equals(ctx.repo.serverPath, ignoreProtocol = true)
                }
                if (mapping == null) {
                    setCurrent(null)
                    return@collectLatest
                }
                mapping.gitRepository.infoFlow()
                    .map { it.currentBranch?.name }
                    .distinctUntilChanged()
                    .collectLatest { branch -> resolveAndSet(ctx, mapping.gitRepository.root, branch) }
            }
        }
    }

    private suspend fun resolveAndSet(ctx: GiteaPRDataContext, repositoryRoot: VirtualFile, branch: String?) {
        if (branch == null) {
            setCurrent(null)
            return
        }
        val repository = GiteaPRRepository(ctx)
        val pr = try {
            repository.loadPullRequests(state = "open").filter { it.head.ref == branch }.maxByOrNull { it.updatedAt }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            LOG.warn("Failed to resolve a PR for current branch '$branch'", e)
            null
        }
        if (pr == null) {
            setCurrent(null)
            return
        }
        val changedFiles = try {
            repository.loadChangedFiles(pr.number.toInt())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            LOG.warn("Failed to load changed files for PR #${pr.number}", e)
            emptyList()
        }

        val prJob = SupervisorJob(cs.coroutineContext[Job])
        val prCs = CoroutineScope(cs.coroutineContext + prJob)
        val discussionsVm = GiteaPRDiscussionsViewModels(project, prCs, pr.number.toInt(), pr.head.sha, repository, pr.mentionCandidates())
        setCurrent(GiteaPRForCurrentBranch(ctx, pr, repository, discussionsVm, changedFiles, repositoryRoot), prJob)
    }

    private fun setCurrent(value: GiteaPRForCurrentBranch?, newJob: Job? = null) {
        prScopeJob?.cancel()
        prScopeJob = newJob
        _current.value = value
    }

    companion object {
        private val LOG = logger<GiteaPRForCurrentBranchService>()
    }
}
