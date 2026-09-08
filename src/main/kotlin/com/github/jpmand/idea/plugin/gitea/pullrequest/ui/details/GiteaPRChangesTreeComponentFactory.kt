package com.github.jpmand.idea.plugin.gitea.pullrequest.ui.details

import com.github.jpmand.idea.plugin.gitea.api.models.GiteaPullRequest
import com.github.jpmand.idea.plugin.gitea.api.rest.pr.GiteaPRFileStatusEnum
import com.github.jpmand.idea.plugin.gitea.pullrequest.data.GiteaPRRepository
import com.github.jpmand.idea.plugin.gitea.util.GiteaBundle
import com.intellij.collaboration.ui.codereview.changes.CodeReviewChangeListComponentFactory
import com.intellij.collaboration.ui.codereview.details.model.CodeReviewChangeList
import com.intellij.collaboration.ui.codereview.details.model.CodeReviewChangeListViewModelBase
import com.intellij.collaboration.util.ChangesSelection
import com.intellij.collaboration.util.RefComparisonChange
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.FilePath
import com.intellij.openapi.vcs.history.ShortVcsRevisionNumber
import com.intellij.openapi.vcs.history.VcsRevisionNumber
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.components.JBPanelWithEmptyText
import com.intellij.ui.components.panels.Wrapper
import com.intellij.vcsUtil.VcsUtil
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.swing.JComponent

/**
 * Renders a PR's changed files as the platform [CodeReviewChangeListComponentFactory] tree
 * (`AsyncChangesTree`). The tree is purely path-based — it needs no local checkout to render —
 * so the [RefComparisonChange]s are synthesized from the REST `pulls/{n}/files` list against the
 * PR's base/head SHAs. Opening a file still goes through the existing REST diff
 * (`GiteaPRDiffVirtualFile`) via [onOpenChange].
 */
@Suppress("UnstableApiUsage")
object GiteaPRChangesTreeComponentFactory {

    fun create(
        cs: CoroutineScope,
        project: Project,
        pr: GiteaPullRequest,
        repository: GiteaPRRepository,
        localRepoRootPath: String?,
        onOpenChange: (Int) -> Unit,
    ): JComponent {
        val wrapper = Wrapper(emptyPanel(GiteaBundle.message("pull.request.details.changes.loading")))

        cs.launch {
            val component: JComponent = try {
                val files = withContext(Dispatchers.IO) { repository.loadChangedFiles(pr.number.toInt()) }
                if (files.isEmpty()) {
                    emptyPanel(GiteaBundle.message("pull.request.details.changes.empty"))
                } else {
                    val before = Sha(pr.base.sha)
                    val after = Sha(pr.head.sha)
                    val changes = files.map { file ->
                        val newPath = filePath(localRepoRootPath, file.filename)
                        val oldPath = file.previousFilename?.let { filePath(localRepoRootPath, it) } ?: newPath
                        when (file.status) {
                            GiteaPRFileStatusEnum.ADDED -> RefComparisonChange(before, null, after, newPath)
                            GiteaPRFileStatusEnum.DELETED -> RefComparisonChange(before, oldPath, after, null)
                            GiteaPRFileStatusEnum.RENAMED, GiteaPRFileStatusEnum.COPIED ->
                                RefComparisonChange(before, oldPath, after, newPath)
                            else -> RefComparisonChange(before, newPath, after, newPath)
                        }
                    }
                    val vm = ChangesTreeVm(cs, project, CodeReviewChangeList(pr.head.sha, changes), onOpenChange)
                    val tree = CodeReviewChangeListComponentFactory.createIn(
                        cs, vm, null, GiteaBundle.message("pull.request.details.changes.empty"),
                    )
                    ScrollPaneFactory.createScrollPane(tree, true)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                thisLogger().warn("Failed to load changed files for PR #${pr.number}", e)
                emptyPanel(GiteaBundle.message("pull.request.details.changes.unavailable"))
            }
            wrapper.setContent(component)
            wrapper.revalidate()
            wrapper.repaint()
        }

        return wrapper
    }

    private fun emptyPanel(text: String): JComponent =
        JBPanelWithEmptyText().apply { emptyText.text = text }

    private fun filePath(repoRootPath: String?, relativePath: String): FilePath {
        val full = if (repoRootPath.isNullOrBlank()) relativePath else "$repoRootPath/$relativePath"
        return VcsUtil.getFilePath(full, false)
    }

    /** Minimal revision number — the path-based tree only needs it for tooltips. */
    private class Sha(private val sha: String) : ShortVcsRevisionNumber {
        override fun asString(): String = sha
        override fun toShortString(): String = sha.take(7)
        override fun compareTo(other: VcsRevisionNumber): Int =
            if (other is Sha) sha.compareTo(other.sha) else 0
    }

    private class ChangesTreeVm(
        parentCs: CoroutineScope,
        override val project: Project,
        changeList: CodeReviewChangeList,
        private val onOpenChange: (Int) -> Unit,
    ) : CodeReviewChangeListViewModelBase(parentCs, changeList) {

        override fun showDiff() {
            val selection = changesSelection.value ?: return
            val idx = when (selection) {
                is ChangesSelection.Precise -> selection.selectedIdx
                is ChangesSelection.Fuzzy -> selection.selectedIdx
            }
            if (idx >= 0) onOpenChange(idx)
        }

        override fun showDiffPreview() = showDiff()
    }
}
