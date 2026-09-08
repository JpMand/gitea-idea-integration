package com.github.jpmand.idea.plugin.gitea.pullrequest.ui.details

import com.github.jpmand.idea.plugin.gitea.api.models.GiteaPullRequest
import com.github.jpmand.idea.plugin.gitea.api.rest.pr.GiteaPRFileStatusEnum
import com.github.jpmand.idea.plugin.gitea.pullrequest.data.GiteaPRRepository
import com.github.jpmand.idea.plugin.gitea.util.GiteaBundle
import com.intellij.collaboration.ui.LoadingLabel
import com.intellij.collaboration.ui.codereview.CodeReviewProgressTreeModelFromDetails
import com.intellij.collaboration.ui.codereview.changes.CodeReviewChangeListComponentFactory
import com.intellij.collaboration.ui.codereview.details.model.CodeReviewChangeList
import com.intellij.collaboration.util.RefComparisonChange
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.FilePath
import com.intellij.openapi.vcs.ProjectLevelVcsManager
import com.intellij.openapi.vcs.history.ShortVcsRevisionNumber
import com.intellij.openapi.vcs.history.VcsRevisionNumber
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.components.panels.Wrapper
import com.intellij.vcsUtil.VcsUtil
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.swing.JComponent

/**
 * Renders a PR's changed files as the platform [CodeReviewChangeListComponentFactory] tree
 * (`AsyncChangesTree`) — mirroring GitLab's `GitLabMergeRequestDetailsChangesComponentFactory`.
 * The tree is path-based (no local checkout needed to render); [RefComparisonChange]s are
 * synthesized from the REST `pulls/{n}/files` list rooted at the project's VCS root so the tree
 * nests by directory and shows the repo-root node + "N files" counters. Viewed checkboxes and
 * directory grouping come from [GiteaPRChangesTreeViewModel]. Opening a file goes through the
 * existing REST diff via [onOpenChange].
 */
@Suppress("UnstableApiUsage")
object GiteaPRChangesTreeComponentFactory {

    fun create(
        cs: CoroutineScope,
        project: Project,
        pr: GiteaPullRequest,
        repository: GiteaPRRepository,
        onOpenChange: (Int) -> Unit,
    ): JComponent {
        val wrapper = Wrapper(LoadingLabel())

        cs.launch {
            val component: JComponent = try {
                val files = withContext(Dispatchers.IO) { repository.loadChangedFiles(pr.number.toInt()) }
                if (files.isEmpty()) {
                    emptyLabel()
                } else {
                    val repoRoot = ProjectLevelVcsManager.getInstance(project).allVersionedRoots.firstOrNull()?.path
                    val before = Sha(pr.base.sha)
                    val after = Sha(pr.head.sha)
                    val relPathByChange = LinkedHashMap<RefComparisonChange, String>()
                    val changes = files.map { file ->
                        val newPath = filePath(repoRoot, file.filename)
                        val oldPath = file.previousFilename?.let { filePath(repoRoot, it) } ?: newPath
                        val change = when (file.status) {
                            GiteaPRFileStatusEnum.ADDED -> RefComparisonChange(before, null, after, newPath)
                            GiteaPRFileStatusEnum.DELETED -> RefComparisonChange(before, oldPath, after, null)
                            GiteaPRFileStatusEnum.RENAMED, GiteaPRFileStatusEnum.COPIED ->
                                RefComparisonChange(before, oldPath, after, newPath)
                            else -> RefComparisonChange(before, newPath, after, newPath)
                        }
                        relPathByChange[change] = file.filename
                        change
                    }
                    val vm = GiteaPRChangesTreeViewModel(
                        cs, project, pr.number.toInt(),
                        CodeReviewChangeList(pr.head.sha, changes), relPathByChange, onOpenChange,
                    )
                    val progressModel = CodeReviewProgressTreeModelFromDetails(cs, vm)
                    val tree = CodeReviewChangeListComponentFactory.createIn(
                        cs, vm, progressModel, GiteaBundle.message("pull.request.details.changes.empty"),
                    )
                    ScrollPaneFactory.createScrollPane(tree, true)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                thisLogger().warn("Failed to load changed files for PR #${pr.number}", e)
                errorLabel()
            }
            wrapper.setContent(component)
            wrapper.revalidate()
            wrapper.repaint()
        }

        return wrapper
    }

    private fun emptyLabel(): JComponent =
        com.intellij.ui.components.JBLabel(GiteaBundle.message("pull.request.details.changes.empty")).apply {
            border = com.intellij.util.ui.JBUI.Borders.empty(12)
        }

    private fun errorLabel(): JComponent =
        com.intellij.ui.components.JBLabel(GiteaBundle.message("pull.request.details.changes.unavailable")).apply {
            border = com.intellij.util.ui.JBUI.Borders.empty(12)
        }

    private fun filePath(repoRootPath: String?, relativePath: String): FilePath =
        if (repoRootPath.isNullOrBlank()) VcsUtil.getFilePath(relativePath, false)
        else VcsUtil.getFilePath(File(repoRootPath, relativePath))

    /** Minimal revision number — the path-based tree only needs it for tooltips. */
    private class Sha(private val sha: String) : ShortVcsRevisionNumber {
        override fun asString(): String = sha
        override fun toShortString(): String = sha.take(7)
        override fun compareTo(other: VcsRevisionNumber): Int =
            if (other is Sha) sha.compareTo(other.sha) else 0
    }
}
