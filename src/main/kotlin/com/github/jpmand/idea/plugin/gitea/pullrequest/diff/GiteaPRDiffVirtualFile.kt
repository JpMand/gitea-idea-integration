package com.github.jpmand.idea.plugin.gitea.pullrequest.diff

import com.github.jpmand.idea.plugin.gitea.api.rest.pr.GiteaPRFileStatusEnum
import com.github.jpmand.idea.plugin.gitea.pullrequest.data.GiteaPRRepository
import com.github.jpmand.idea.plugin.gitea.pullrequest.review.GiteaPRDiscussionsViewModels
import com.intellij.collaboration.ui.codereview.diff.AsyncDiffRequestProcessorFactory
import com.intellij.collaboration.util.KeyValuePair
import com.intellij.diff.editor.DiffViewerVirtualFile
import com.intellij.diff.impl.DiffEditorViewer
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.FileStatus
import com.intellij.openapi.vcs.LocalFilePath
import com.intellij.openapi.vcs.changes.ui.PresentableChange
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.flowOf

/**
 * Bare identity — like [com.github.jpmand.idea.plugin.gitea.pullrequest.GiteaPRTimelineVirtualFile],
 * [equals]/[hashCode] fold in the context (account + repo) so [FileEditorManager][com.intellij.openapi.fileEditor.FileEditorManager]
 * correctly reuses/focuses an already-open diff tab for the same PR under the same context,
 * instead of always opening a new one (the previous behavior here, before this identity was added).
 */
@Suppress("UnstableApiUsage")
class GiteaPRDiffVirtualFile(
    private val prNumber: Int,
    private val cs: CoroutineScope,
    private val project: Project,
    private val repository: GiteaPRRepository,
    private val vm: GiteaPRDiffViewModel,
    private val discussionsVm: GiteaPRDiscussionsViewModels,
) : DiffViewerVirtualFile("gitea-pr-$prNumber") {

    override fun isValid(): Boolean = !project.isDisposed

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GiteaPRDiffVirtualFile) return false
        return prNumber == other.prNumber &&
            project == other.project &&
            repository.accountId == other.repository.accountId &&
            repository.repositoryCoordinates.repositoryPath == other.repository.repositoryCoordinates.repositoryPath
    }

    override fun hashCode(): Int {
        var result = prNumber
        result = 31 * result + project.hashCode()
        result = 31 * result + repository.accountId.hashCode()
        result = 31 * result + repository.repositoryCoordinates.repositoryPath.hashCode()
        return result
    }

    override fun createViewer(project: Project): DiffEditorViewer =
        AsyncDiffRequestProcessorFactory.createIn(
            cs, project,
            flowOf(vm),
            createContext = { listOf(KeyValuePair(GiteaPRDiscussionsViewModels.CONTEXT_KEY, discussionsVm)) },
            changePresenter = { fileVm ->
                object : PresentableChange {
                    override fun getFilePath() = LocalFilePath(fileVm.file.filename, false)
                    override fun getFileStatus(): FileStatus = when (fileVm.file.status) {
                        GiteaPRFileStatusEnum.ADDED -> FileStatus.ADDED
                        GiteaPRFileStatusEnum.DELETED -> FileStatus.DELETED
                        GiteaPRFileStatusEnum.RENAMED -> FileStatus.MODIFIED
                        else -> FileStatus.MODIFIED
                    }
                }
            }
        )
}
