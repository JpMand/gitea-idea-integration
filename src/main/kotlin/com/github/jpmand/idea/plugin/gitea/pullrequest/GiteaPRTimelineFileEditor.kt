package com.github.jpmand.idea.plugin.gitea.pullrequest

import com.github.jpmand.idea.plugin.gitea.api.models.GiteaUser
import com.github.jpmand.idea.plugin.gitea.data.GiteaImageLoader
import com.github.jpmand.idea.plugin.gitea.pullrequest.ui.timeline.GiteaPRTimelineComponentFactory
import com.github.jpmand.idea.plugin.gitea.pullrequest.ui.timeline.GiteaPRTimelineItemComponentFactory
import com.github.jpmand.idea.plugin.gitea.pullrequest.ui.timeline.GiteaPRTimelineViewModel
import com.github.jpmand.idea.plugin.gitea.pullrequest.ui.toolwindow.GiteaPRCommitSelectionRequests
import com.github.jpmand.idea.plugin.gitea.util.GiteaUtil
import com.intellij.collaboration.ui.icon.AsyncImageIconsProvider
import com.intellij.collaboration.ui.icon.CachingIconsProvider
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.VirtualFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import java.beans.PropertyChangeListener
import javax.swing.JComponent

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
        project, avatarIconsProvider, { m -> GiteaUtil.safeConvertMarkdownToHtml(m) }, headSha = file.pr.head.sha,
        currentUserLogin = file.ctx.account.name,
        onEditComment = { id, body -> file.repository.editComment(id, body); vm.reload() },
        onDeleteComment = { id -> file.repository.deleteComment(id); vm.reload() },
        onOpenCommit = { sha ->
            project.service<GiteaPRCommitSelectionRequests>().request(file.pr, file.repository, file.ctx, sha)
        },
        onReplyToThread = { threadId, body ->
            file.repository.replyToComment(file.pr.number.toInt(), threadId, body)
            vm.reload()
        },
        currentUser = vm.currentUser,
        mentionCandidates = vm.mentionCandidates,
    )

    private val component: JComponent =
        GiteaPRTimelineComponentFactory.create(cs, vm, itemFactory, avatarIconsProvider) { vm.reload() }

    override fun getComponent(): JComponent = component
    override fun getPreferredFocusedComponent(): JComponent? = null
    override fun getName(): String = "${file.pr.title} #${file.pr.number}"
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
