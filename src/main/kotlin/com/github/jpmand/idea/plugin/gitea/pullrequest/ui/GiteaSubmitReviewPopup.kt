package com.github.jpmand.idea.plugin.gitea.pullrequest.ui

import com.github.jpmand.idea.plugin.gitea.pullrequest.review.GiteaPRDiscussionsViewModels
import com.github.jpmand.idea.plugin.gitea.pullrequest.review.GiteaReviewVerdict
import com.github.jpmand.idea.plugin.gitea.pullrequest.review.reviewSubmitProblem
import com.github.jpmand.idea.plugin.gitea.util.GiteaBundle
import com.intellij.collaboration.messages.CollaborationToolsBundle
import com.intellij.collaboration.ui.HorizontalListPanel
import com.intellij.collaboration.ui.codereview.list.error.ErrorStatusPresenter
import com.intellij.collaboration.ui.codereview.review.CodeReviewSubmitPopupHandler
import com.intellij.collaboration.ui.codereview.review.CodeReviewSubmitViewModel
import com.intellij.collaboration.ui.util.bindDisabledIn
import com.intellij.collaboration.ui.util.bindVisibilityIn
import com.intellij.openapi.project.Project
import com.intellij.util.ui.InlineIconButton
import com.intellij.util.ui.JBUI
import icons.CollaborationToolsIcons
import java.awt.event.ActionListener
import javax.swing.JButton
import javax.swing.JComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/**
 * Backs [GiteaSubmitReviewPopup]: a review body plus a verdict, submitted through
 * [GiteaPRDiscussionsViewModels.submit] (which finishes a pending review or sends the local
 * drafts as a new one). [onDone] closes the popup; it runs after a successful submission, a
 * discard, or a cancel.
 */
@Suppress("UnstableApiUsage")
internal class GiteaSubmitReviewViewModel(
    private val project: Project,
    private val discussionsVm: GiteaPRDiscussionsViewModels,
    private val onDone: () -> Unit,
) : CodeReviewSubmitViewModel {

    val viewerIsAuthor: Boolean get() = discussionsVm.viewerIsAuthor

    /** Comments that go out with the review: local drafts, or those already on a pending review. */
    override val draftCommentsCount: StateFlow<Int> =
        discussionsVm.pendingReview.value?.let { MutableStateFlow(it.commentsCount).asStateFlow() }
            ?: discussionsVm.draftCommentsCount

    override val text: MutableStateFlow<String> = MutableStateFlow(discussionsVm.pendingReview.value?.body.orEmpty())

    override val isBusy: StateFlow<Boolean> get() = discussionsVm.isSubmittingReview

    private val _error = MutableStateFlow<Throwable?>(null)
    override val error: StateFlow<Throwable?> = _error.asStateFlow()

    /** Whether [verdict] can be submitted with the current body and comments. */
    fun canSubmit(verdict: GiteaReviewVerdict): Flow<Boolean> =
        combine(text, draftCommentsCount) { body, count -> reviewSubmitProblem(verdict, body, count) == null }

    fun submit(verdict: GiteaReviewVerdict) {
        _error.value = null
        discussionsVm.submit(verdict, text.value, onSuccess = onDone, onError = { _error.value = it })
    }

    /** Asks for confirmation, then discards the drafts and any pending review. */
    fun discard() {
        confirmAndCancelReview(project, discussionsVm)
        onDone()
    }

    override fun cancel() = onDone()
}

/** The platform's review-submit popup (same one the GitHub plugin uses), with Gitea's verdicts. */
@Suppress("UnstableApiUsage")
internal object GiteaSubmitReviewPopup : CodeReviewSubmitPopupHandler<GiteaSubmitReviewViewModel>() {

    override fun CoroutineScope.createActionsComponent(vm: GiteaSubmitReviewViewModel): JComponent {
        val cs = this
        fun verdictButton(bundleKey: String, verdict: GiteaReviewVerdict): JButton =
            JButton(GiteaBundle.message(bundleKey)).apply {
                isOpaque = false
                bindDisabledIn(cs, combine(vm.isBusy, vm.canSubmit(verdict)) { busy, ok -> busy || !ok })
                addActionListener { vm.submit(verdict) }
            }
        return HorizontalListPanel(ACTIONS_GAP).apply {
            // Gitea rejects approving or requesting changes on your own PR.
            if (!vm.viewerIsAuthor) {
                add(verdictButton("pull.request.action.approve", GiteaReviewVerdict.APPROVE))
                add(verdictButton("pull.request.action.request.changes", GiteaReviewVerdict.REQUEST_CHANGES).apply {
                    toolTipText = GiteaBundle.message("pull.request.review.submit.error.request.changes.body")
                })
            }
            add(verdictButton("pull.request.action.comment", GiteaReviewVerdict.COMMENT))
        }
    }

    override fun createTitleActionsComponentIn(cs: CoroutineScope, vm: GiteaSubmitReviewViewModel): JComponent {
        val close = super.createTitleActionsComponentIn(cs, vm)
        val discard = InlineIconButton(
            icon = CollaborationToolsIcons.Delete,
            hoveredIcon = CollaborationToolsIcons.DeleteHovered,
            tooltip = GiteaBundle.message("pull.request.action.cancel.review"),
        ).apply {
            border = JBUI.Borders.empty(5)
            bindDisabledIn(cs, vm.isBusy)
            bindVisibilityIn(cs, vm.draftCommentsCount.map { it > 0 })
            actionListener = ActionListener { vm.discard() }
        }
        return HorizontalListPanel(TITLE_ACTIONS_GAP).apply {
            add(discard)
            add(close)
        }
    }

    override val errorPresenter: ErrorStatusPresenter<Throwable> by lazy {
        ErrorStatusPresenter.simpleHTML(
            CollaborationToolsBundle.message("review.submit.failed"),
            descriptionProvider = { it.localizedMessage },
        )
    }
}
