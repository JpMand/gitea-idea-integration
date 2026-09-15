package com.github.jpmand.idea.plugin.gitea.pullrequest.diff

import com.github.jpmand.idea.plugin.gitea.api.rest.dto.CreatePullReviewOptions
import com.github.jpmand.idea.plugin.gitea.api.rest.dto.SubmitPullReviewOptions
import com.github.jpmand.idea.plugin.gitea.pullrequest.GiteaPullRequestsSettings
import com.github.jpmand.idea.plugin.gitea.pullrequest.ui.editor.GiteaPRDiffEditorModel
import com.github.jpmand.idea.plugin.gitea.pullrequest.ui.editor.GiteaPRInlayComponentsFactory
import com.github.jpmand.idea.plugin.gitea.pullrequest.review.GiteaPRDiscussionsViewModels
import com.github.jpmand.idea.plugin.gitea.util.GiteaBundle
import com.intellij.collaboration.async.launchNow
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.components.service
import com.intellij.collaboration.ui.codereview.diff.viewer.showCodeReview
import com.intellij.collaboration.ui.codereview.editor.ReviewInEditorUtil
import com.intellij.diff.DiffContext
import com.intellij.diff.DiffExtension
import com.intellij.diff.FrameDiffTool
import com.intellij.diff.requests.DiffRequest
import com.intellij.diff.tools.util.base.DiffViewerBase
import com.intellij.openapi.Disposable
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.util.Disposer
import icons.CollaborationToolsIcons
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * DiffExtension that wires gutter controls and inline review inlays into any
 * Gitea PR diff viewer that carries a [GiteaPRDiscussionsViewModels] context key.
 *
 * Registered via `<diff.DiffExtension>` in plugin.xml.
 */
@Suppress("UnstableApiUsage")
class GiteaPRDiffExtension : DiffExtension() {

    override fun onViewerCreated(viewer: FrameDiffTool.DiffViewer, context: DiffContext, request: DiffRequest) {
        if (viewer !is DiffViewerBase) return
        val project = context.project ?: return
        if (!project.service<GiteaPullRequestsSettings>().editorReviewEnabled) return
        val discussionsVm = context.getUserData(GiteaPRDiscussionsViewModels.CONTEXT_KEY) ?: return
        val fileVm = request.getUserData(GiteaPRDiffFileViewModel.CONTEXT_KEY) ?: return

        val job = SupervisorJob()
        Disposer.register(viewer as Disposable, Disposable { job.cancel() })
        val cs = CoroutineScope(job + Dispatchers.Main)

        cs.launchNow {
            viewer.showCodeReview(
                modelFactory = { editor, side, locationToLine, lineToLocation, _ ->
                    launchNow {
                        ReviewInEditorUtil.showReviewToolbarWithActions(discussionsVm, editor, submitReviewAction(discussionsVm))
                    }
                    GiteaPRDiffEditorModel(this, project, fileVm.file, side, discussionsVm, locationToLine, lineToLocation)
                },
                rendererFactory = { inlayModel ->
                    GiteaPRInlayComponentsFactory.createRenderer(project, this, inlayModel, discussionsVm)
                }
            )
        }
    }

    /**
     * A compact "N drafts ▾" / "Finish review ▾" action in the editor's inspection-widget corner
     * strip (the only per-editor toolbar hook the platform gives — a secondary, lightweight
     * affordance; the Details tab's review composer, with a full body field, stays primary). Opens
     * a quick-verdict popup that submits with an empty body — compose a body first in the Details
     * tab if you want one.
     */
    private fun submitReviewAction(discussionsVm: GiteaPRDiscussionsViewModels): AnAction =
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
                val group = DefaultActionGroup(
                    if (pending == null) {
                        listOf(
                            verdictAction("pull.request.action.comment") { discussionsVm.submitReview(CreatePullReviewOptions.Event.COMMENT, "") },
                            verdictAction("pull.request.action.approve") { discussionsVm.submitReview(CreatePullReviewOptions.Event.APPROVED, "") },
                            verdictAction("pull.request.action.request.changes") {
                                discussionsVm.submitReview(CreatePullReviewOptions.Event.REQUESTCHANGES, "")
                            },
                        )
                    } else {
                        listOf(
                            verdictAction("pull.request.action.comment") { discussionsVm.submitPendingReview(SubmitPullReviewOptions.Event.COMMENT, "") },
                            verdictAction("pull.request.action.approve") { discussionsVm.submitPendingReview(SubmitPullReviewOptions.Event.APPROVED, "") },
                            verdictAction("pull.request.action.request.changes") {
                                discussionsVm.submitPendingReview(SubmitPullReviewOptions.Event.REQUESTCHANGES, "")
                            },
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
}
