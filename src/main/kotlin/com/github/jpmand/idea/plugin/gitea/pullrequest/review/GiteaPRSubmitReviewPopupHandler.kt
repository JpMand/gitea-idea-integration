package com.github.jpmand.idea.plugin.gitea.pullrequest.review

import com.github.jpmand.idea.plugin.gitea.api.rest.models.pr.GiteaReviewEventEnum
import com.github.jpmand.idea.plugin.gitea.util.GiteaBundle
import com.intellij.collaboration.messages.CollaborationToolsBundle
import com.intellij.collaboration.ui.codereview.list.error.ErrorStatusPresenter
import com.intellij.collaboration.ui.codereview.review.CodeReviewSubmitPopupHandler
import com.intellij.collaboration.ui.util.bindDisabledIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.combine
import java.awt.FlowLayout
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel

/**
 * Popup handler for the "Submit Review" popup.
 *
 * Extends [CodeReviewSubmitPopupHandler], which provides the popup shell (title row, review body
 * editor, error panel, draft comment count badge). This object only needs to supply the action
 * buttons row and an error presenter.
 *
 * Button enable/disable rules:
 * - **Approve**: disabled only while the submit request is in flight.
 * - **Request Changes / Comment**: also disabled when there is no text body and no draft comments
 *   (nothing to submit).
 */
@Suppress("UnstableApiUsage")
object GiteaPRSubmitReviewPopupHandler : CodeReviewSubmitPopupHandler<GiteaPRReviewViewModel>() {

    override val errorPresenter: ErrorStatusPresenter<Throwable> = ErrorStatusPresenter.simple(
        CollaborationToolsBundle.message("review.submit.failed"),
        descriptionProvider = { e -> e.message },
    )

    override fun CoroutineScope.createActionsComponent(vm: GiteaPRReviewViewModel): JComponent {
        val cs = this
        val hasInput = combine(vm.text, vm.draftCommentsCount) { text, count ->
            text.isNotBlank() || count > 0
        }

        val approveButton = JButton(GiteaBundle.message("pull.request.review.submit.approve")).apply {
            bindDisabledIn(cs, vm.isBusy)
            addActionListener { vm.submit(GiteaReviewEventEnum.APPROVED) }
        }
        val requestChangesButton = JButton(GiteaBundle.message("pull.request.review.submit.request.changes")).apply {
            bindDisabledIn(cs, combine(vm.isBusy, hasInput) { busy, input -> busy || !input })
            addActionListener { vm.submit(GiteaReviewEventEnum.REQUEST_CHANGES) }
        }
        val commentButton = JButton(GiteaBundle.message("pull.request.review.submit.comment")).apply {
            bindDisabledIn(cs, combine(vm.isBusy, hasInput) { busy, input -> busy || !input })
            addActionListener { vm.submit(GiteaReviewEventEnum.COMMENT) }
        }

        return JPanel(FlowLayout(FlowLayout.LEFT, ACTIONS_GAP, 0)).apply {
            isOpaque = false
            add(approveButton)
            add(requestChangesButton)
            add(commentButton)
        }
    }
}
