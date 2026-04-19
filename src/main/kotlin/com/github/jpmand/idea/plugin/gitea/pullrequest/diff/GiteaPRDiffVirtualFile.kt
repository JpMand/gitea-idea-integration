package com.github.jpmand.idea.plugin.gitea.pullrequest.diff

import com.github.jpmand.idea.plugin.gitea.api.rest.models.pr.GiteaPRFileStatusEnum
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

@Suppress("UnstableApiUsage")
class GiteaPRDiffVirtualFile(
    private val prNumber: Int,
    private val cs: CoroutineScope,
    private val project: Project,
    private val vm: GiteaPRDiffViewModel,
    private val discussionsVm: GiteaPRDiscussionsViewModels,
) : DiffViewerVirtualFile("gitea-pr-$prNumber") {

    override fun isValid(): Boolean = !project.isDisposed

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
