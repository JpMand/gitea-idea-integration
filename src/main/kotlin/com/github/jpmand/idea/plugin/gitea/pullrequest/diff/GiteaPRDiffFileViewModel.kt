package com.github.jpmand.idea.plugin.gitea.pullrequest.diff

import com.github.jpmand.idea.plugin.gitea.api.rest.models.pr.GiteaPRFileStatusEnum
import com.github.jpmand.idea.plugin.gitea.pullrequest.data.GiteaPRRepository
import com.intellij.collaboration.ui.codereview.diff.model.AsyncDiffViewModel
import com.intellij.collaboration.util.ComputedResult
import com.intellij.diff.DiffContentFactory
import com.intellij.diff.requests.DiffRequest
import com.intellij.diff.requests.SimpleDiffRequest
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest

@Suppress("UnstableApiUsage")
class GiteaPRDiffFileViewModel(
    parentCs: CoroutineScope,
    private val project: Project,
    private val repository: GiteaPRRepository,
    val file: GiteaPRChangedFile,
    private val baseSha: String,
    private val headSha: String,
) : AsyncDiffViewModel {

    private val cs = CoroutineScope(parentCs.coroutineContext + SupervisorJob(parentCs.coroutineContext[Job]))

    private val _reloadTrigger = MutableStateFlow(0)

    @OptIn(ExperimentalCoroutinesApi::class)
    override val request: StateFlow<ComputedResult<DiffRequest>?> =
        _reloadTrigger.transformLatest {
            emit(ComputedResult.loading())
            try {
                emit(ComputedResult.success(buildDiffRequest()))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                emit(ComputedResult.failure(e))
            }
        }.stateIn(cs, SharingStarted.Eagerly, null)

    override fun reloadRequest() {
        _reloadTrigger.value++
    }

    private suspend fun buildDiffRequest(): DiffRequest {
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

        return SimpleDiffRequest(
            file.filename,
            baseDoc, headDoc,
            "Base (${baseSha.take(7)})",
            "Head (${headSha.take(7)})",
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GiteaPRDiffFileViewModel) return false
        return file == other.file && baseSha == other.baseSha && headSha == other.headSha
    }

    override fun hashCode(): Int = 31 * (31 * file.hashCode() + baseSha.hashCode()) + headSha.hashCode()
}
