package com.github.jpmand.idea.plugin.gitea.pullrequest.ui.details

import com.github.jpmand.idea.plugin.gitea.api.models.GiteaPullRequest
import com.github.jpmand.idea.plugin.gitea.api.rest.dto.Commit
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
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.panels.Wrapper
import com.intellij.util.ui.JBUI
import com.intellij.vcsUtil.VcsUtil
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.swing.JComponent

/**
 * Renders a PR's changed files as the platform [CodeReviewChangeListComponentFactory] tree
 * (`AsyncChangesTree`) — mirroring GitLab's `GitLabMergeRequestDetailsChangesComponentFactory`.
 * The tree reloads whenever [selectedCommitFlow] changes: `null` = the whole PR (base..head),
 * a specific commit = just that commit's files. Viewed checkboxes and directory grouping come
 * from [GiteaPRChangesTreeViewModel]; opening a file goes through the existing REST diff via
 * [onOpenChange] (called with the repo-relative path).
 */
@Suppress("UnstableApiUsage")
object GiteaPRChangesTreeComponentFactory {

    fun create(
        cs: CoroutineScope,
        project: Project,
        pr: GiteaPullRequest,
        repository: GiteaPRRepository,
        selectedCommitFlow: Flow<Commit?>,
        onOpenChange: (String) -> Unit,
    ): JComponent {
        val wrapper = Wrapper(LoadingLabel())

        cs.launch {
            selectedCommitFlow.distinctUntilChangedBy { it?.sha }.collectLatest { selectedCommit ->
                wrapper.setContent(LoadingLabel())
                wrapper.repaint()

                val component: JComponent = try {
                    val files = withContext(Dispatchers.IO) {
                        val sha = selectedCommit?.sha
                        if (sha == null) repository.loadChangedFiles(pr.number.toInt())
                        else repository.loadCommitChangedFiles(sha)
                    }
                    if (files.isEmpty()) {
                        label(GiteaBundle.message("pull.request.details.changes.empty"))
                    } else {
                        val beforeSha = selectedCommit?.parents?.firstOrNull()?.sha ?: pr.base.sha
                        val afterSha = selectedCommit?.sha ?: pr.head.sha
                        val repoRoot = ProjectLevelVcsManager.getInstance(project).allVersionedRoots.firstOrNull()?.path
                        val before = Sha(beforeSha)
                        val after = Sha(afterSha)
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
                            CodeReviewChangeList(afterSha, changes), relPathByChange, onOpenChange,
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
                    label(GiteaBundle.message("pull.request.details.changes.unavailable"))
                }
                wrapper.setContent(component)
                wrapper.revalidate()
                wrapper.repaint()
            }
        }

        return wrapper
    }

    private fun label(text: String): JComponent =
        JBLabel(text).apply { border = JBUI.Borders.empty(12) }

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
