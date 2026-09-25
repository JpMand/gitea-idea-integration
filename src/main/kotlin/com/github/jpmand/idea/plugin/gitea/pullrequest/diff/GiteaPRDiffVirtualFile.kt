package com.github.jpmand.idea.plugin.gitea.pullrequest.diff

import com.github.jpmand.idea.plugin.gitea.api.rest.pr.GiteaPRFileStatusEnum
import com.github.jpmand.idea.plugin.gitea.pullrequest.data.GiteaPRRepository
import com.github.jpmand.idea.plugin.gitea.pullrequest.review.GiteaPRDiscussionsViewModels
import com.github.jpmand.idea.plugin.gitea.pullrequest.ui.discussionsViewOptionsAction
import com.github.jpmand.idea.plugin.gitea.pullrequest.ui.submitReviewAction
import com.intellij.collaboration.ui.codereview.diff.AsyncDiffRequestProcessorFactory
import com.intellij.collaboration.util.KeyValuePair
import com.intellij.diff.editor.DiffViewerVirtualFile
import com.intellij.diff.impl.DiffEditorViewer
import com.intellij.diff.util.DiffUserDataKeys
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
) : DiffViewerVirtualFile("Diff for Pull Request #$prNumber") {

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

    // GitHub-style placement (GHPRDiffService.createDiffContext): the review-submit control lands
    // in the diff header's own toolbar via DiffUserDataKeys.CONTEXT_ACTIONS, not in a floating
    // per-editor overlay (that mechanism — ReviewInEditorUtil.showReviewToolbarWithActions, via
    // GiteaReviewToolbar.launchReviewToolbar — stays reserved for the regular-editor surface, which
    // has no diff header to place it in). No separate GHPRDiffService-style service class is
    // needed here: this is the only createViewer/processor-construction call site in the plugin,
    // so the extra responsibilities that class carries (combined-diff toggle, sharing one scope
    // across multiple call sites) don't apply.
    override fun createViewer(project: Project): DiffEditorViewer =
        AsyncDiffRequestProcessorFactory.createIn(
            cs, project,
            flowOf(vm),
            createContext = {
                listOf(
                    KeyValuePair(GiteaPRDiscussionsViewModels.CONTEXT_KEY, discussionsVm),
                    KeyValuePair(
                        DiffUserDataKeys.CONTEXT_ACTIONS,
                        listOf(discussionsViewOptionsAction(discussionsVm), submitReviewAction(project, discussionsVm)),
                    ),
                )
            },
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
