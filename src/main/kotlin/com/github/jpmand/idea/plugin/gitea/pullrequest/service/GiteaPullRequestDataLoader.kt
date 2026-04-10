package com.github.jpmand.idea.plugin.gitea.pullrequest.service

import com.github.jpmand.idea.plugin.gitea.api.models.GiteaChangedFile
import com.github.jpmand.idea.plugin.gitea.api.models.GiteaDiffComment
import com.github.jpmand.idea.plugin.gitea.api.models.GiteaPullRequest
import com.github.jpmand.idea.plugin.gitea.api.models.GiteaPullRequestComment
import com.github.jpmand.idea.plugin.gitea.api.models.GiteaPullRequestReview
import com.github.jpmand.idea.plugin.gitea.api.rest.models.commit.GiteaCommitDTO
import com.github.jpmand.idea.plugin.gitea.api.rest.repoGetAllPullRequestDiffComments
import com.github.jpmand.idea.plugin.gitea.api.rest.repoGetPullRequest
import com.github.jpmand.idea.plugin.gitea.api.rest.repoListPullRequestComments
import com.github.jpmand.idea.plugin.gitea.api.rest.repoListPullRequestCommits
import com.github.jpmand.idea.plugin.gitea.api.rest.repoListPullRequestFiles
import com.github.jpmand.idea.plugin.gitea.api.rest.repoListPullRequestReviews
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Lazy-loads and caches per-PR data.
 * Created by [GiteaPullRequestsProjectService.getDataLoader] and scoped to a single PR.
 */
@Suppress("UnstableApiUsage")
class GiteaPullRequestDataLoader(
    private val cs: CoroutineScope,
    private val service: GiteaPullRequestsProjectService,
    val owner: String,
    val repo: String,
    val index: Int
) {
    private val _prState = MutableStateFlow<Result<GiteaPullRequest>?>(null)
    val prState: StateFlow<Result<GiteaPullRequest>?> = _prState

    private val _reviewsState = MutableStateFlow<Result<List<GiteaPullRequestReview>>?>(null)
    val reviewsState: StateFlow<Result<List<GiteaPullRequestReview>>?> = _reviewsState

    private val _commentsState = MutableStateFlow<Result<List<GiteaPullRequestComment>>?>(null)
    val commentsState: StateFlow<Result<List<GiteaPullRequestComment>>?> = _commentsState

    private val _filesState = MutableStateFlow<Result<List<GiteaChangedFile>>?>(null)
    val filesState: StateFlow<Result<List<GiteaChangedFile>>?> = _filesState

    private val _commitsState = MutableStateFlow<Result<List<GiteaCommitDTO>>?>(null)
    val commitsState: StateFlow<Result<List<GiteaCommitDTO>>?> = _commitsState

    private val _diffCommentsState = MutableStateFlow<Result<List<GiteaDiffComment>>?>(null)
    val diffCommentsState: StateFlow<Result<List<GiteaDiffComment>>?> = _diffCommentsState

    init {
        loadAll()
    }

    fun loadAll() {
        reloadPr()
        reloadReviews()
        reloadComments()
        reloadFiles()
        reloadCommits()
        reloadDiffComments()
    }

    fun reloadPr() {
        cs.launch {
            _prState.value = runCatching {
                val api = service.getOrLoadApiForActiveRepo() ?: error("No API available")
                api.repoGetPullRequest(owner, repo, index).toPullRequest()
            }
        }
    }

    fun reloadReviews() {
        cs.launch {
            _reviewsState.value = runCatching {
                val api = service.getOrLoadApiForActiveRepo() ?: error("No API available")
                api.repoListPullRequestReviews(owner, repo, index).map { it.toReview() }
            }
        }
    }

    fun reloadComments() {
        cs.launch {
            _commentsState.value = runCatching {
                val api = service.getOrLoadApiForActiveRepo() ?: error("No API available")
                api.repoListPullRequestComments(owner, repo, index).map { it.toComment() }
            }
        }
    }

    fun reloadFiles() {
        cs.launch {
            _filesState.value = runCatching {
                val api = service.getOrLoadApiForActiveRepo() ?: error("No API available")
                api.repoListPullRequestFiles(owner, repo, index).map { it.toChangedFile() }
            }
        }
    }

    fun reloadCommits() {
        cs.launch {
            _commitsState.value = runCatching {
                val api = service.getOrLoadApiForActiveRepo() ?: error("No API available")
                api.repoListPullRequestCommits(owner, repo, index)
            }
        }
    }

    fun reloadDiffComments() {
        cs.launch {
            _diffCommentsState.value = runCatching {
                val api = service.getOrLoadApiForActiveRepo() ?: error("No API available")
                api.repoGetAllPullRequestDiffComments(owner, repo, index).map { it.toDiffComment() }
            }
        }
    }
}
