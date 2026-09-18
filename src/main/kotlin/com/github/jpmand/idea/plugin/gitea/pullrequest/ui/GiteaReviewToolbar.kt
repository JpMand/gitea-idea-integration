package com.github.jpmand.idea.plugin.gitea.pullrequest.ui

import com.github.jpmand.idea.plugin.gitea.api.rest.dto.CreatePullReviewOptions
import com.github.jpmand.idea.plugin.gitea.api.rest.dto.SubmitPullReviewOptions
import com.github.jpmand.idea.plugin.gitea.pullrequest.review.GiteaPRDiscussionsViewModels
import com.github.jpmand.idea.plugin.gitea.util.GiteaBundle
import com.github.jpmand.idea.plugin.gitea.util.GiteaUtil
import com.intellij.collaboration.async.launchNow
import com.intellij.collaboration.ui.codereview.editor.ReviewInEditorUtil
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ex.EditorMarkupModel
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import icons.CollaborationToolsIcons
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay

/**
 * Shows the review toolbar in its own [SupervisorJob], isolated from whatever sibling
 * gutter-controls/inlay-rendering coroutines the caller also runs in the same `coroutineScope` —
 * both [com.github.jpmand.idea.plugin.gitea.pullrequest.diff.GiteaPRDiffExtension] (diff tab) and
 * the Phase-9 in-editor controller (regular project editor) call this.
 * [ReviewInEditorUtil.showReviewToolbarWithActions] throws (`"Editor markup model is not
 * available"`) if `editor.markupModel` isn't yet an `EditorMarkupModel` — most often true for a
 * freshly-created editor, right when it's created. Left as a plain sibling coroutine, that throw
 * would cancel the whole shared scope via structured concurrency, silently killing gutter
 * controls and inlays along with the toolbar. A short poll-and-retry covers the race instead of
 * letting the first attempt fail outright.
 */
fun CoroutineScope.launchReviewToolbar(project: Project, editor: Editor, discussionsVm: GiteaPRDiscussionsViewModels) {
    val toolbarScope = CoroutineScope(coroutineContext + SupervisorJob(coroutineContext[Job]))
    toolbarScope.launchNow {
        try {
            var attempt = 0
            while (editor.markupModel !is EditorMarkupModel && attempt < 20) {
                delay(50)
                attempt++
            }
            ReviewInEditorUtil.showReviewToolbarWithActions(discussionsVm, editor, submitReviewAction(project, discussionsVm))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            GiteaUtil.LOG.warn("Failed to show PR review toolbar", e)
        }
    }
}

/**
 * A compact "N drafts ▾" / "Finish review ▾" action for the editor's inspection-widget corner
 * strip (the only per-editor toolbar hook the platform gives — a secondary, lightweight
 * affordance; the Details tab's review composer, with a full body field, stays primary). Opens a
 * quick-verdict popup that submits with an empty body — compose a body first in the Details tab
 * if you want one.
 */
fun submitReviewAction(project: Project, discussionsVm: GiteaPRDiscussionsViewModels): AnAction =
    object : AnAction() {
        override fun getActionUpdateThread() = ActionUpdateThread.BGT

        override fun update(e: AnActionEvent) {
            val pending = discussionsVm.pendingReview.value
            e.presentation.text = if (pending != null) {
                GiteaBundle.message("pull.request.review.finish.short")
            } else {
                GiteaBundle.message("pull.request.review.composer.draft.count", discussionsVm.draftComments.value.size)
            }
            e.presentation.icon = CollaborationToolsIcons.Review.CommentUnread
            e.presentation.putClientProperty(ActionUtil.SHOW_TEXT_IN_TOOLBAR, true)
        }

        override fun actionPerformed(e: AnActionEvent) {
            val pending = discussionsVm.pendingReview.value
            val cancelReview = verdictAction("pull.request.action.cancel.review") { confirmAndCancelReview(project, discussionsVm) }
            val group = DefaultActionGroup(
                if (pending == null) {
                    listOf(
                        verdictAction("pull.request.action.comment") { discussionsVm.submitReview(CreatePullReviewOptions.Event.COMMENT, "") },
                        verdictAction("pull.request.action.approve") { discussionsVm.submitReview(CreatePullReviewOptions.Event.APPROVED, "") },
                        verdictAction("pull.request.action.request.changes") {
                            discussionsVm.submitReview(CreatePullReviewOptions.Event.REQUESTCHANGES, "")
                        },
                        cancelReview,
                    )
                } else {
                    listOf(
                        verdictAction("pull.request.action.comment") { discussionsVm.submitPendingReview(SubmitPullReviewOptions.Event.COMMENT, "") },
                        verdictAction("pull.request.action.approve") { discussionsVm.submitPendingReview(SubmitPullReviewOptions.Event.APPROVED, "") },
                        verdictAction("pull.request.action.request.changes") {
                            discussionsVm.submitPendingReview(SubmitPullReviewOptions.Event.REQUESTCHANGES, "")
                        },
                        cancelReview,
                    )
                },
            )
            JBPopupFactory.getInstance()
                .createActionGroupPopup(null, group, e.dataContext, JBPopupFactory.ActionSelectionAid.SPEEDSEARCH, true)
                .showInBestPositionFor(e.dataContext)
        }
    }

private fun verdictAction(bundleKey: String, run: () -> Unit): AnAction =
    object : AnAction(GiteaBundle.message(bundleKey)) {
        override fun getActionUpdateThread() = ActionUpdateThread.BGT
        override fun actionPerformed(e: AnActionEvent) = run()
    }
