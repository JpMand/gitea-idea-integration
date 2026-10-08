package com.github.jpmand.idea.plugin.gitea.pullrequest.ui.details

import com.github.jpmand.idea.plugin.gitea.api.models.GiteaCommit
import com.github.jpmand.idea.plugin.gitea.api.models.GiteaPullRequest
import com.github.jpmand.idea.plugin.gitea.api.rest.pr.GiteaPRFileStatusEnum
import com.github.jpmand.idea.plugin.gitea.pullrequest.data.GiteaPRRepository
import com.github.jpmand.idea.plugin.gitea.pullrequest.diff.GiteaPRDiffTarget
import com.github.jpmand.idea.plugin.gitea.pullrequest.review.GiteaPRDiscussionsViewModels
import com.github.jpmand.idea.plugin.gitea.util.GiteaBundle
import com.intellij.collaboration.ui.CollaborationToolsUIUtil
import com.intellij.collaboration.ui.LoadingLabel
import com.intellij.collaboration.ui.codereview.CodeReviewProgressTreeModelFromDetails
import com.intellij.collaboration.ui.codereview.changes.CodeReviewChangeListComponentFactory
import com.intellij.collaboration.ui.codereview.details.model.CodeReviewChangeList
import com.intellij.collaboration.ui.codereview.list.error.ErrorStatusPanelFactory
import com.intellij.collaboration.ui.codereview.list.error.ErrorStatusPresenter
import com.intellij.collaboration.ui.util.swingAction
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
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File
import javax.swing.JComponent

/**
 * Renders a PR's changed files as the platform [CodeReviewChangeListComponentFactory] tree
 * (`AsyncChangesTree`) — mirroring GitLab's `GitLabMergeRequestDetailsChangesComponentFactory`.
 * The tree reloads whenever [selectedCommitFlow] changes: `null` = the whole PR (base..head),
 * a specific commit = just that commit's files. It also reloads when [prFlow] brings a new head
 * or merge base. Directory grouping and per-file comment counts come
 * from [GiteaPRChangesTreeViewModel]; opening a file goes through the existing REST diff via
 * [onOpenChange], called with the diff to open (the selected commit's, or the whole PR's) and the
 * repo-relative path.
 */
@Suppress("UnstableApiUsage")
object GiteaPRChangesTreeComponentFactory {

    fun create(
        cs: CoroutineScope,
        project: Project,
        prFlow: StateFlow<GiteaPullRequest>,
        repository: GiteaPRRepository,
        discussionsVm: GiteaPRDiscussionsViewModels,
        selectedCommitFlow: Flow<GiteaCommit?>,
        onOpenChange: (GiteaPRDiffTarget, String) -> Unit,
    ): JComponent {
        val wrapper = Wrapper(LoadingLabel())
        // Bumped by the error panel's Retry to load the same selection again.
        val retries = MutableStateFlow(0)

        cs.launch {
            combine(
                selectedCommitFlow.distinctUntilChangedBy { it?.sha },
                prFlow.distinctUntilChangedBy { Triple(it.number, it.diffBaseSha, it.head.sha) },
                retries,
            ) { commit, pr, _ -> commit to pr }.collectLatest { (selectedCommit, pr) ->
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
                        val beforeSha = selectedCommit?.firstParentSha ?: pr.diffBaseSha
                        val afterSha = selectedCommit?.sha ?: pr.head.sha
                        val diffTarget = if (selectedCommit == null) GiteaPRDiffTarget.PullRequest
                        else GiteaPRDiffTarget.Commit(afterSha, beforeSha)
                        val repoRoot = ProjectLevelVcsManager.getInstance(project).getAllVersionedRoots().firstOrNull()?.path
                        val before = Sha(beforeSha)
                        val after = Sha(afterSha)
                        val relPathByChange = LinkedHashMap<RefComparisonChange, String>()
                        val previousRelPathByChange = LinkedHashMap<RefComparisonChange, String>()
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
                            file.previousFilename?.let { previousRelPathByChange[change] = it }
                            change
                        }
                        val vm = GiteaPRChangesTreeViewModel(
                            cs, project, CodeReviewChangeList(afterSha, changes),
                            relPathByChange, previousRelPathByChange, discussionsVm,
                            onOpenChange = { relPath -> onOpenChange(diffTarget, relPath) },
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
                    // An error, not "no changes": platform error styling, centred, with Retry.
                    CollaborationToolsUIUtil.moveToCenter(
                        ErrorStatusPanelFactory.create(
                            e,
                            ErrorStatusPresenter.simple(
                                GiteaBundle.message("pull.request.details.changes.unavailable"),
                                descriptionProvider = { it.message },
                                actionProvider = { swingAction(GiteaBundle.message("pull.request.error.retry")) { retries.value++ } },
                            ),
                            ErrorStatusPanelFactory.Alignment.CENTER,
                        ),
                    )
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
        else VcsUtil.getFilePath(File(repoRootPath, relativePath), false)

    /** Minimal revision number — the path-based tree only needs it for tooltips. */
    private class Sha(private val sha: String) : ShortVcsRevisionNumber {
        override fun asString(): String = sha
        override fun toShortString(): String = sha.take(7)
        override fun compareTo(other: VcsRevisionNumber): Int =
            if (other is Sha) sha.compareTo(other.sha) else 0
    }
}
