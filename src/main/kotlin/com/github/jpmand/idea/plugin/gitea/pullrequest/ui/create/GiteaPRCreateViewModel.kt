package com.github.jpmand.idea.plugin.gitea.pullrequest.ui.create

import com.github.jpmand.idea.plugin.gitea.api.models.GiteaLabel
import com.github.jpmand.idea.plugin.gitea.api.models.GiteaPullRequest
import com.github.jpmand.idea.plugin.gitea.api.models.GiteaUser
import com.github.jpmand.idea.plugin.gitea.pullrequest.data.GiteaPRRepository
import com.github.jpmand.idea.plugin.gitea.ui.GiteaSettings
import com.github.jpmand.idea.plugin.gitea.util.GiteaGitRepositoryMapping
import com.intellij.collaboration.ui.codereview.create.CodeReviewTitleDescriptionViewModel
import com.intellij.collaboration.util.ComputedResult
import com.intellij.dvcs.DvcsUtil
import com.intellij.dvcs.push.PushSpec
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.coroutineToIndicator
import com.intellij.openapi.project.Project
import git4idea.GitBranch
import git4idea.GitLocalBranch
import git4idea.GitPushUtil
import git4idea.GitRemoteBranch
import git4idea.GitStandardRemoteBranch
import git4idea.GitVcs
import git4idea.push.GitPushOperation
import git4idea.push.GitPushSource
import git4idea.push.GitPushSupport
import git4idea.push.GitPushTarget
import git4idea.remote.hosting.GitCodeReviewUtils
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val LOG = logger<GiteaPRCreateViewModel>()

/**
 * The "New Pull Request" tab: which branch goes into which, the title, description, WIP flag,
 * reviewers and labels, and what the pull request would contain, read from local git (see
 * [GiteaPRLocalComparison]) so a branch that isn't pushed yet can be previewed. [create] pushes the
 * head branch when needed, then opens the pull request on the server.
 */
class GiteaPRCreateViewModel(
    val project: Project,
    private val cs: CoroutineScope,
    private val repository: GiteaPRRepository,
    /** The git repository and remote the pull request is opened on (the PR context's). */
    val mapping: GiteaGitRepositoryMapping,
    initialHead: GitBranch?,
    private val onCreated: (GiteaPullRequest) -> Unit,
) : CodeReviewTitleDescriptionViewModel {

    private val root get() = mapping.gitRepository.root

    // ── Branches ──────────────────────────────────────────────────────────

    private val _baseBranch = MutableStateFlow<GitRemoteBranch?>(null)
    val baseBranch: StateFlow<GitRemoteBranch?> = _baseBranch.asStateFlow()

    private val _headBranch = MutableStateFlow(initialHead ?: mapping.gitRepository.currentBranch)
    val headBranch: StateFlow<GitBranch?> = _headBranch.asStateFlow()

    fun setBaseBranch(branch: GitRemoteBranch?) {
        _baseBranch.value = branch
    }

    fun setHeadBranch(branch: GitBranch?) {
        _headBranch.value = branch
    }

    // ── Title & description ───────────────────────────────────────────────

    private val _title = MutableStateFlow("")
    override val titleText: StateFlow<String> = _title.asStateFlow()

    private val _description = MutableStateFlow("")
    override val descriptionText: StateFlow<String> = _description.asStateFlow()

    private val _templateLoading = MutableStateFlow(false)
    override val isTemplateLoading: StateFlow<Boolean> = _templateLoading.asStateFlow()

    // Once edited, a field keeps the user's text when the branches change.
    private var titleEdited = false
    private var descriptionEdited = false

    override fun setTitle(text: String) {
        if (text == _title.value) return
        titleEdited = true
        _title.value = text
    }

    override fun setDescription(text: String) {
        if (text == _description.value) return
        descriptionEdited = true
        _description.value = text
    }

    val isWip: MutableStateFlow<Boolean> = MutableStateFlow(false)

    // ── Reviewers & labels ────────────────────────────────────────────────

    val reviewers: MutableStateFlow<List<GiteaUser>> = MutableStateFlow(emptyList())
    val labels: MutableStateFlow<List<GiteaLabel>> = MutableStateFlow(emptyList())

    /** Who can be asked to review: never the signed-in user, who opens the pull request. */
    suspend fun loadPossibleReviewers(): List<GiteaUser> =
        repository.loadPossibleReviewers(GiteaSettings.getInstance().allUsersArePotentialReviewers)
            .filterNot { it.login.equals(repository.accountLogin, ignoreCase = true) }

    suspend fun loadLabels(): List<GiteaLabel> = repository.loadLabels()

    // ── What the pull request would contain ───────────────────────────────

    /** What the branches lead to; see [Check] for the order they're shown in. */
    sealed interface Check {
        data object NoCommits : Check
        data class AlreadyExists(val pullRequest: GiteaPullRequest) : Check
        data class Ready(val needsPush: Boolean, val mergesCleanly: Boolean?) : Check
    }

    private val _comparison = MutableStateFlow<ComputedResult<GiteaPRLocalComparison>?>(null)
    /** Null until both branches are chosen. */
    val comparison: StateFlow<ComputedResult<GiteaPRLocalComparison>?> = _comparison.asStateFlow()

    private val _check = MutableStateFlow<ComputedResult<Check>?>(null)
    val check: StateFlow<ComputedResult<Check>?> = _check.asStateFlow()

    // ── Creating ──────────────────────────────────────────────────────────

    sealed interface CreationState {
        data object Pushing : CreationState
        data object Creating : CreationState
        data class Failed(val error: Throwable) : CreationState
    }

    private val _creation = MutableStateFlow<CreationState?>(null)
    val creation: StateFlow<CreationState?> = _creation.asStateFlow()
    private var creationJob: Job? = null

    init {
        cs.launch {
            val defaultBranch = try {
                repository.loadDefaultBranch()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                LOG.warn("Couldn't load the default branch of ${mapping.repository}", e)
                null
            }
            if (_baseBranch.value == null) {
                val name = defaultBranch ?: mapping.gitRepository.branches.remoteBranches
                    .firstOrNull { it.remote == mapping.gitRemote && it.nameForRemoteOperations in listOf("main", "master") }
                    ?.nameForRemoteOperations
                _baseBranch.value = name?.let { remoteBranch(it) }
            }
        }
        cs.launch {
            combine(_baseBranch, _headBranch) { base, head -> base to head }.collectLatest { (base, head) ->
                if (base == null || head == null) {
                    _comparison.value = null
                    _check.value = null
                    return@collectLatest
                }
                refresh(base, head)
            }
        }
    }

    private fun remoteBranch(name: String): GitRemoteBranch =
        mapping.gitRepository.branches.remoteBranches.firstOrNull { it.remote == mapping.gitRemote && it.nameForRemoteOperations == name }
            ?: GitStandardRemoteBranch(mapping.gitRemote, name)

    /** The branch name the pull request's head has on the server. */
    private fun remoteHeadName(head: GitBranch): String = when (head) {
        is GitRemoteBranch -> head.nameForRemoteOperations
        is GitLocalBranch -> pushTarget(head).branch.nameForRemoteOperations
        else -> head.name
    }

    private suspend fun refresh(base: GitRemoteBranch, head: GitBranch) {
        _comparison.value = ComputedResult.loading()
        _check.value = ComputedResult.loading()
        val baseRef = base.nameForLocalOperations
        val headRef = head.name
        try {
            // The remote-tracking branch can be behind the server; a stale base would preview
            // commits that are already merged.
            val baseRefspec = "+refs/heads/${base.nameForRemoteOperations}:refs/remotes/${mapping.gitRemote.name}/${base.nameForRemoteOperations}"
            try {
                GitCodeReviewUtils.fetch(mapping.gitRepository, mapping.gitRemote, baseRefspec)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                LOG.debug("Couldn't fetch $baseRefspec, comparing with the local copy", e)
            }
            val comparison = withContext(Dispatchers.IO) { GiteaPRLocalComparison.compute(project, root, baseRef, headRef) }
                ?: error("$headRef and $baseRef have no common history")
            _comparison.value = ComputedResult.success(comparison)
            applyDefaults(head, baseRef, comparison)

            _check.value = ComputedResult.success(
                when {
                    comparison.commits.isEmpty() -> Check.NoCommits
                    else -> repository.findOpenPullRequest(base.nameForRemoteOperations, remoteHeadName(head))
                        ?.let { Check.AlreadyExists(it) }
                        ?: Check.Ready(
                            needsPush = needsPush(head),
                            mergesCleanly = withContext(Dispatchers.IO) { GiteaPRLocalComparison.mergesCleanly(project, root, baseRef, comparison.headSha) },
                        )
                },
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            LOG.warn("Couldn't compare $headRef with $baseRef", e)
            _comparison.value = ComputedResult.failure(e)
            _check.value = ComputedResult.failure(e)
        }
    }

    private suspend fun applyDefaults(head: GitBranch, baseRef: String, comparison: GiteaPRLocalComparison) {
        if (titleEdited && descriptionEdited) return
        _templateLoading.value = true
        val template = try {
            withContext(Dispatchers.IO) {
                PR_TEMPLATE_CANDIDATES.firstNotNullOfOrNull { GiteaPRLocalComparison.readFile(project, root, baseRef, it) }
            }
        } finally {
            _templateLoading.value = false
        }
        val (title, description) = defaultTitleAndDescription(
            head.name,
            comparison.commits.map { GiteaCommitMessage(it.fullMessage) },
            template,
        )
        if (!titleEdited) _title.value = title
        if (!descriptionEdited) _description.value = description
    }

    /**
     * Where [head] goes on the PR context's remote: its configured push target or tracked branch of
     * the same name, else a branch of the same name. That one may already exist, e.g. pushed by an
     * earlier pull request while the local branch tracks another one (`git checkout -b x origin/main`).
     */
    private fun pushTarget(head: GitLocalBranch): GitPushTarget =
        GitPushUtil.findPushTarget(mapping.gitRepository, mapping.gitRemote, head)
            ?: mapping.gitRepository.branches.remoteBranches
                .firstOrNull { it.remote == mapping.gitRemote && it.nameForRemoteOperations == head.name }
                ?.let { GitPushTarget(it, false) }
            ?: GitPushTarget(GitStandardRemoteBranch(mapping.gitRemote, head.name), true)

    private fun needsPush(head: GitBranch): Boolean {
        if (head !is GitLocalBranch) return false
        val target = pushTarget(head)
        if (target.isNewBranchCreated) return true
        val branches = mapping.gitRepository.branches
        return branches.getHash(head) != branches.getHash(target.branch)
    }

    /** Pushes the head branch if needed, then opens the pull request. */
    fun create() {
        val base = _baseBranch.value ?: return
        val head = _headBranch.value ?: return
        val title = _title.value.trim().takeIf { it.isNotEmpty() } ?: return
        if (creationJob?.isActive == true) return
        val check = _check.value?.result?.getOrNull()
        if (check !is Check.Ready) return
        creationJob = cs.launch {
            try {
                val headName = if (head is GitLocalBranch && check.needsPush) {
                    _creation.value = CreationState.Pushing
                    push(head)
                } else {
                    remoteHeadName(head)
                }
                _creation.value = CreationState.Creating
                val pr = repository.createPullRequest(
                    base = base.nameForRemoteOperations,
                    head = headName,
                    title = if (isWip.value) withWipPrefix(title) else title,
                    body = _description.value,
                    labelIds = labels.value.map { it.id },
                    reviewers = reviewers.value.map { it.login },
                )
                LOG.info("Created PR #${pr.number}: $headName into ${base.nameForRemoteOperations}")
                _creation.value = null
                onCreated(pr)
            } catch (e: CancellationException) {
                _creation.value = null
                throw e
            } catch (e: Exception) {
                LOG.warn("Couldn't create the pull request from ${head.name} into ${base.nameForRemoteOperations}", e)
                _creation.value = CreationState.Failed(e)
            }
        }
    }

    /**
     * Pushes [head] to [pushTarget] and returns the remote branch name. As the IDE's Push does, a new
     * remote branch becomes the upstream only when [head] doesn't track one yet.
     */
    private suspend fun push(head: GitLocalBranch): String {
        val target = pushTarget(head)
        LOG.info("Pushing ${head.name} to ${target.branch.nameForLocalOperations} before creating the pull request")
        val pushSupport = DvcsUtil.getPushSupport(GitVcs.getInstance(project)) as GitPushSupport
        val spec = PushSpec(GitPushSource.create(head), target)
        val operation = GitPushOperation(project, pushSupport, mapOf(mapping.gitRepository to spec), null, false, false)
        val result = withContext(Dispatchers.IO) {
            coroutineToIndicator { operation.execute().results[mapping.gitRepository] }
        } ?: error("Pushing ${head.name} gave no result")
        result.error?.let { error(it) }
        return target.branch.nameForRemoteOperations
    }
}
