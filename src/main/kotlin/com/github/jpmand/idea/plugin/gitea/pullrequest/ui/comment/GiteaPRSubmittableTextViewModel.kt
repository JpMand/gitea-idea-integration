package com.github.jpmand.idea.plugin.gitea.pullrequest.ui.comment

import com.github.jpmand.idea.plugin.gitea.api.models.GiteaUser
import com.github.jpmand.idea.plugin.gitea.util.GiteaBundle
import com.intellij.collaboration.ui.codereview.CodeReviewChatItemUIUtil
import com.intellij.collaboration.ui.codereview.comment.CodeReviewCommentTextFieldFactory
import com.intellij.collaboration.ui.codereview.comment.CodeReviewSubmittableTextViewModelBase
import com.intellij.collaboration.ui.codereview.comment.CommentInputActionsComponentFactory
import com.intellij.collaboration.ui.codereview.timeline.comment.CommentTextFieldFactory
import com.intellij.collaboration.ui.icon.IconsProvider
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import java.awt.event.ActionEvent
import javax.swing.AbstractAction
import javax.swing.JComponent

/**
 * A markdown comment editor (platform [CodeReviewSubmittableTextViewModelBase] +
 * [CodeReviewCommentTextFieldFactory]) that posts a new top-level PR comment via [onSubmit] and
 * clears the field on success. A failed submission leaves the draft text in place and surfaces
 * through the platform's own busy/error UI, driven by
 * [state][com.intellij.collaboration.ui.codereview.comment.CodeReviewSubmittableTextViewModel.state].
 */
@Suppress("UnstableApiUsage")
class GiteaPRSubmittableTextViewModel(
    project: Project,
    cs: CoroutineScope,
    private val onSubmit: suspend (String) -> Unit,
) : CodeReviewSubmittableTextViewModelBase(project, cs, "") {

    fun submitComment() {
        if (text.value.isBlank()) return
        submit { body ->
            onSubmit(body)
            text.value = ""
        }
    }
}

@Suppress("UnstableApiUsage")
object GiteaPRCommentFieldFactory {

    fun create(
        cs: CoroutineScope,
        vm: GiteaPRSubmittableTextViewModel,
        avatars: IconsProvider<GiteaUser>,
        iconUser: GiteaUser,
    ): JComponent {
        val submitAction = object : AbstractAction(GiteaBundle.message("pull.request.action.comment")) {
            override fun actionPerformed(e: ActionEvent?) = vm.submitComment()
        }
        val config = CommentInputActionsComponentFactory.Config(
            primaryAction = MutableStateFlow(submitAction),
            secondaryActions = MutableStateFlow(emptyList()),
            additionalActions = MutableStateFlow(emptyList()),
            cancelAction = MutableStateFlow(null),
            submitHint = MutableStateFlow(GiteaBundle.message("pull.request.timeline.comment.placeholder")),
        )
        val iconConfig = CommentTextFieldFactory.IconConfig.of(
            CodeReviewChatItemUIUtil.ComponentType.FULL, avatars, iconUser,
        )
        return CodeReviewCommentTextFieldFactory.createIn(cs, vm, config, iconConfig)
    }
}
