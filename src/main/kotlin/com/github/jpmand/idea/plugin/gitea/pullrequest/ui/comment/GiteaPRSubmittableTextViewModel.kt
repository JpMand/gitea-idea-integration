package com.github.jpmand.idea.plugin.gitea.pullrequest.ui.comment

import com.github.jpmand.idea.plugin.gitea.api.models.GiteaUser
import com.github.jpmand.idea.plugin.gitea.pullrequest.ui.comment.mention.GITEA_MENTION_CANDIDATES_KEY
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
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
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
    /** `false` for the suggested-change composer, where the typed text is only an optional
     * explanation on top of an already-nonempty suggestion block — GitHub itself allows posting a
     * suggestion with no comment text at all. */
    private val requireNonBlank: Boolean = true,
    private val onSubmit: suspend (String) -> Unit,
) : CodeReviewSubmittableTextViewModelBase(project, cs, "") {

    fun submitComment() {
        if (requireNonBlank && text.value.isBlank()) return
        submit { body ->
            onSubmit(body)
            text.value = ""
        }
    }
}

@Suppress("UnstableApiUsage")
object GiteaPRCommentFieldFactory {

    /** A secondary, immediately-firing option next to the primary submit button — rendered as a
     * dropdown entry on the same [com.intellij.ui.components.JBOptionButton] the platform's
     * [CommentInputActionsComponentFactory] already builds from [CommentInputActionsComponentFactory.Config.secondaryActions].
     * Unlike the merge/review-verdict split buttons elsewhere in this plugin, there's no
     * select-then-confirm step here — each option is already a complete, independent action. */
    data class SecondaryAction(val labelKey: String, val onSubmit: () -> Unit)

    fun create(
        cs: CoroutineScope,
        vm: GiteaPRSubmittableTextViewModel,
        avatars: IconsProvider<GiteaUser>,
        iconUser: GiteaUser,
        /** Repo collaborators for `@`-mention completion; `null` skips wiring it up. */
        mentionCandidates: StateFlow<List<GiteaUser>>? = null,
        /** Adds a "Cancel" action next to Submit — used by dismissible composers (e.g. a new
         * inline-comment inlay); `null` (the default) omits it, matching every existing caller. */
        onCancel: (() -> Unit)? = null,
        /** Bundle key for the primary submit button's label — defaults to "Comment". */
        primaryActionLabelKey: String = "pull.request.action.comment",
        /** An optional extra option in the primary button's dropdown — `null` (the default) omits
         * it, matching every existing caller. */
        secondaryAction: SecondaryAction? = null,
    ): JComponent {
        val submitAction = object : AbstractAction(GiteaBundle.message(primaryActionLabelKey)) {
            override fun actionPerformed(e: ActionEvent?) = vm.submitComment()
        }
        val secondaryActions = secondaryAction?.let { action ->
            listOf(object : AbstractAction(GiteaBundle.message(action.labelKey)) {
                override fun actionPerformed(e: ActionEvent?) = action.onSubmit()
            })
        }.orEmpty()
        val cancelAction = onCancel?.let { cancel ->
            object : AbstractAction(GiteaBundle.message("pull.request.action.cancel")) {
                override fun actionPerformed(e: ActionEvent?) = cancel()
            }
        }
        val config = CommentInputActionsComponentFactory.Config(
            primaryAction = MutableStateFlow(submitAction),
            secondaryActions = MutableStateFlow(secondaryActions),
            additionalActions = MutableStateFlow(emptyList()),
            cancelAction = MutableStateFlow(cancelAction),
            submitHint = MutableStateFlow(GiteaBundle.message("pull.request.timeline.comment.placeholder")),
        )
        val iconConfig = CommentTextFieldFactory.IconConfig.of(
            CodeReviewChatItemUIUtil.ComponentType.FULL, avatars, iconUser,
        )
        return CodeReviewCommentTextFieldFactory.createIn(cs, vm, config, iconConfig) { editor ->
            if (mentionCandidates != null) {
                cs.launch { mentionCandidates.collect { editor.putUserData(GITEA_MENTION_CANDIDATES_KEY, it) } }
            }
        }
    }
}
