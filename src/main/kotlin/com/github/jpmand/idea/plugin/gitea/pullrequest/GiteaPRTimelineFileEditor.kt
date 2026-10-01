package com.github.jpmand.idea.plugin.gitea.pullrequest

import com.github.jpmand.idea.plugin.gitea.api.models.GiteaUser
import com.github.jpmand.idea.plugin.gitea.data.GiteaImageLoader
import com.github.jpmand.idea.plugin.gitea.pullrequest.review.GiteaPRReviewChanges
import com.github.jpmand.idea.plugin.gitea.pullrequest.ui.timeline.GiteaPRTimelineComponentFactory
import com.github.jpmand.idea.plugin.gitea.pullrequest.ui.timeline.GiteaPRTimelineItemComponentFactory
import com.github.jpmand.idea.plugin.gitea.pullrequest.ui.timeline.GiteaPRTimelineViewModel
import com.github.jpmand.idea.plugin.gitea.pullrequest.ui.toolwindow.GiteaPRCommitSelectionRequests
import com.github.jpmand.idea.plugin.gitea.pullrequest.ui.toolwindow.GiteaPRShowDiffRequests
import com.github.jpmand.idea.plugin.gitea.util.GiteaUtil
import com.intellij.collaboration.ui.icon.AsyncImageIconsProvider
import com.intellij.collaboration.ui.icon.CachingIconsProvider
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.VirtualFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.beans.PropertyChangeListener
import java.util.Date
import javax.swing.JComponent

private val LOG = logger<GiteaPRTimelineFileEditor>()

/** Editor tab rendering a PR's activity timeline (Conversation). */
@Suppress("UnstableApiUsage")
class GiteaPRTimelineFileEditor(
    private val project: Project,
    private val file: GiteaPRTimelineVirtualFile,
) : UserDataHolderBase(), FileEditor {

    private val cs = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val vm = GiteaPRTimelineViewModel(cs, project, file.pr, file.repository)
    private val avatarIconsProvider =
        CachingIconsProvider(AsyncImageIconsProvider<GiteaUser>(cs, GiteaImageLoader(file.ctx.api)))
    private val itemFactory = GiteaPRTimelineItemComponentFactory(
        project, avatarIconsProvider, { m -> GiteaUtil.safeConvertMarkdownToHtml(m) },
        currentUserLogin = file.ctx.account.name,
        onEditComment = { id, body ->
            LOG.info("PR #${file.pr.number}: editing comment $id")
            file.repository.editComment(id, body)
            vm.updateCommentBody(id, body, Date())
            notifyReviewChanged()
        },
        onDeleteComment = { id ->
            LOG.info("PR #${file.pr.number}: deleting comment $id")
            file.repository.deleteComment(id)
            vm.removeComment(id)
            notifyReviewChanged()
        },
        onOpenCommit = { sha ->
            project.service<GiteaPRCommitSelectionRequests>().request(file.pr, file.repository, file.ctx, sha)
        },
        onShowThreadDiff = { thread ->
            project.service<GiteaPRShowDiffRequests>().request(file.pr, file.repository, file.ctx, thread)
        },
        onReplyToThread = { threadId, body ->
            LOG.info("PR #${file.pr.number}: replying to thread $threadId")
            val reply = file.repository.replyToComment(file.pr.number.toInt(), threadId, body)
            vm.appendReply(threadId, reply)
            notifyReviewChanged()
        },
        onResolveThread = { threadId ->
            LOG.info("PR #${file.pr.number}: resolving thread $threadId")
            file.repository.resolveComment(threadId)
            vm.updateThreadResolved(threadId, resolved = true)
            notifyReviewChanged()
        },
        onUnresolveThread = { threadId ->
            LOG.info("PR #${file.pr.number}: unresolving thread $threadId")
            file.repository.unresolveComment(threadId)
            vm.updateThreadResolved(threadId, resolved = false)
            notifyReviewChanged()
        },
        currentUser = vm.currentUser,
        mentionCandidates = vm.mentionCandidates,
    )

    /** Lets this PR's diff and editor review surfaces pick up a change made in the timeline. */
    private fun notifyReviewChanged() =
        project.service<GiteaPRReviewChanges>().notifyChanged(file.pr.number.toInt(), vm)

    init {
        // Keep the tab's title in step with the PR's, which each reload re-fetches.
        cs.launch {
            vm.pr.map { it.title }.distinctUntilChanged().collect { title ->
                if (title == file.title) return@collect
                file.title = title
                FileEditorManagerEx.getInstanceEx(project).updateFilePresentation(file)
            }
        }
    }

    private val component: JComponent =
        GiteaPRTimelineComponentFactory.create(cs, vm, itemFactory, avatarIconsProvider) { vm.reload() }

    override fun getComponent(): JComponent = component
    override fun getPreferredFocusedComponent(): JComponent? = null
    override fun getName(): String = file.presentableName
    override fun setState(state: FileEditorState) {}
    override fun isModified(): Boolean = false
    override fun isValid(): Boolean = !project.isDisposed
    override fun addPropertyChangeListener(listener: PropertyChangeListener) {}
    override fun removePropertyChangeListener(listener: PropertyChangeListener) {}
    override fun getFile(): VirtualFile = file

    override fun dispose() {
        cs.cancel()
    }
}
