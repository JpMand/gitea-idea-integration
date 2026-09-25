package com.github.jpmand.idea.plugin.gitea.pullrequest.ui

import com.github.jpmand.idea.plugin.gitea.pullrequest.review.GiteaPRDiscussionsViewModels
import com.github.jpmand.idea.plugin.gitea.util.GiteaBundle
import com.github.jpmand.idea.plugin.gitea.util.GiteaUtil
import com.intellij.collaboration.async.launchNow
import com.intellij.collaboration.ui.codereview.editor.ReviewInEditorUtil
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.actionSystem.ex.CustomComponentAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ex.EditorMarkupModel
import com.intellij.openapi.project.Project
import icons.CollaborationToolsIcons
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Shared with [com.github.jpmand.idea.plugin.gitea.pullrequest.diff.GiteaPRDiffExtension]'s own
 * retry loop around the sibling gutter-controls/inlay install — both are working around the same
 * "editor not fully initialized yet" race. */
internal const val REVIEW_UI_INSTALL_RETRY_ATTEMPTS = 20
internal const val REVIEW_UI_INSTALL_RETRY_DELAY_MS = 50L

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
            while (editor.markupModel !is EditorMarkupModel && attempt < REVIEW_UI_INSTALL_RETRY_ATTEMPTS) {
                delay(REVIEW_UI_INSTALL_RETRY_DELAY_MS)
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
 * A compact "N drafts" / "Finish review" action for the editor's inspection-widget corner strip
 * (the only per-editor toolbar hook the platform gives). Opens [GiteaSubmitReviewPopup] under the
 * button: a review body plus Approve / Request Changes / Comment and a discard button, the same
 * popup the GitHub plugin shows.
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
            // The clicked toolbar button, so the popup opens under it rather than under the
            // whole editor/diff panel the data context points at.
            val component = e.inputEvent?.component
                ?: e.presentation.getClientProperty(CustomComponentAction.COMPONENT_KEY)
                ?: e.getData(PlatformDataKeys.CONTEXT_COMPONENT)
            var popupJob: Job? = null
            val vm = GiteaSubmitReviewViewModel(project, discussionsVm) { popupJob?.cancel() }
            popupJob = discussionsVm.scope.launch(Dispatchers.Main) {
                if (component != null) GiteaSubmitReviewPopup.show(vm, component) else GiteaSubmitReviewPopup.show(vm, project)
            }
        }
    }
