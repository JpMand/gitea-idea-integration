package com.github.jpmand.idea.plugin.gitea.pullrequest.diff

import com.github.jpmand.idea.plugin.gitea.api.rest.pr.GiteaPRFileStatusEnum
import com.github.jpmand.idea.plugin.gitea.pullrequest.data.GiteaPRRepository
import com.github.jpmand.idea.plugin.gitea.util.GiteaBundle
import com.intellij.collaboration.ui.codereview.diff.model.AsyncDiffViewModel
import com.intellij.collaboration.ui.codereview.diff.model.DiffViewerScrollRequest
import com.intellij.collaboration.ui.codereview.diff.model.DiffViewerScrollRequestProducer
import com.intellij.collaboration.util.ComputedResult
import com.intellij.diff.DiffContentFactory
import com.intellij.diff.requests.DiffRequest
import com.intellij.diff.requests.SimpleDiffRequest
import com.intellij.diff.util.Side
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest

private val LOG = logger<GiteaPRDiffFileViewModel>()

@Suppress("UnstableApiUsage")
class GiteaPRDiffFileViewModel(
    parentCs: CoroutineScope,
    private val project: Project,
    private val repository: GiteaPRRepository,
    val file: GiteaPRChangedFile,
    /** Which version of the PR this file's diff belongs to; [baseSha]..[headSha] are its ends. */
    val target: GiteaPRDiffTarget,
    /** The PR's merge base — see [showsThread]. */
    private val mergeBaseSha: String,
    private val baseSha: String,
    private val headSha: String,
) : AsyncDiffViewModel, DiffViewerScrollRequestProducer {

    companion object {
        val CONTEXT_KEY: Key<GiteaPRDiffFileViewModel> = Key.create("gitea.pr.diff.file.vm")
    }

    private val cs = CoroutineScope(parentCs.coroutineContext + SupervisorJob(parentCs.coroutineContext[Job]))

    private val _reloadTrigger = MutableStateFlow(0)

    // Conflated and consumed once: the diff viewer starts collecting only when this file is shown,
    // so a request made just before is kept until then, and isn't replayed on coming back to the file.
    private val scrollChannel = Channel<DiffViewerScrollRequest>(Channel.CONFLATED)
    override val scrollRequests: Flow<DiffViewerScrollRequest> = scrollChannel.receiveAsFlow()

    /** Scrolls the diff viewer of this file to [request] once it's shown. */
    fun requestScroll(request: DiffViewerScrollRequest) {
        scrollChannel.trySend(request)
    }

    // Loaded when the diff viewer first shows this file, then kept: a PR's file view models are all
    // created when the PR opens, and loading eagerly fetched every changed file's base and head.
    @OptIn(ExperimentalCoroutinesApi::class)
    override val request: StateFlow<ComputedResult<DiffRequest>?> =
        _reloadTrigger.transformLatest {
            emit(ComputedResult.loading())
            try {
                emit(ComputedResult.success(buildDiffRequest()))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                LOG.warn("Couldn't load the diff of ${file.filename} ($baseSha..$headSha)", e)
                emit(ComputedResult.failure(e))
            }
        }.stateIn(cs, SharingStarted.Lazily, null)

    /** Whether a thread of a review made on [reviewCommitId] belongs in this diff, on [side]. */
    fun showsThread(reviewCommitId: String?, side: Side): Boolean = target.showsThread(reviewCommitId, side, mergeBaseSha)

    override fun reloadRequest() {
        _reloadTrigger.value++
    }

    private suspend fun buildDiffRequest(): DiffRequest {
        LOG.trace("Loading the diff of ${file.filename} (${file.status}, $baseSha..$headSha)")
        val baseFilename = if (file.status == GiteaPRFileStatusEnum.RENAMED) {
            file.previousFilename ?: file.filename
        } else {
            file.filename
        }

        val baseContent = if (file.status != GiteaPRFileStatusEnum.ADDED) {
            repository.loadFileContent(baseFilename, baseSha)
        } else {
            ""
        }

        val headContent = if (file.status != GiteaPRFileStatusEnum.DELETED) {
            repository.loadFileContent(file.filename, headSha)
        } else {
            ""
        }

        val fileType = FileTypeManager.getInstance().getFileTypeByFileName(file.filename)
        val baseDoc = DiffContentFactory.getInstance().create(baseContent, fileType)
        val headDoc = DiffContentFactory.getInstance().create(headContent, fileType)

        val base = baseSha.take(7)
        val head = headSha.take(7)
        val (baseTitle, headTitle) = when (target) {
            GiteaPRDiffTarget.PullRequest ->
                GiteaBundle.message("pull.request.diff.side.base", base) to GiteaBundle.message("pull.request.diff.side.head", head)
            is GiteaPRDiffTarget.Commit ->
                GiteaBundle.message("pull.request.diff.side.parent", base) to GiteaBundle.message("pull.request.diff.side.commit", head)
            is GiteaPRDiffTarget.PullRequestAt ->
                GiteaBundle.message("pull.request.diff.side.base", base) to GiteaBundle.message("pull.request.diff.side.reviewed", head)
        }
        return SimpleDiffRequest(file.filename, baseDoc, headDoc, baseTitle, headTitle)
            .also { it.putUserData(CONTEXT_KEY, this) }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GiteaPRDiffFileViewModel) return false
        return file == other.file && target == other.target && baseSha == other.baseSha && headSha == other.headSha
    }

    override fun hashCode(): Int = 31 * (31 * (31 * file.hashCode() + target.hashCode()) + baseSha.hashCode()) + headSha.hashCode()
}
