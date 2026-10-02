package com.github.jpmand.idea.plugin.gitea.pullrequest.diff

import com.github.jpmand.idea.plugin.gitea.pullrequest.ui.editor.GiteaPRDiffEditorModel
import com.github.jpmand.idea.plugin.gitea.pullrequest.ui.editor.GiteaPRInlayComponentsFactory
import com.github.jpmand.idea.plugin.gitea.pullrequest.review.GiteaPRDiscussionsViewModels
import com.github.jpmand.idea.plugin.gitea.pullrequest.ui.REVIEW_UI_INSTALL_RETRY_ATTEMPTS
import com.github.jpmand.idea.plugin.gitea.pullrequest.ui.REVIEW_UI_INSTALL_RETRY_DELAY_MS
import com.intellij.collaboration.async.launchNow
import com.intellij.collaboration.ui.codereview.diff.viewer.showCodeReview
import com.intellij.diff.DiffContext
import com.intellij.diff.DiffExtension
import com.intellij.diff.FrameDiffTool
import com.intellij.diff.requests.DiffRequest
import com.intellij.diff.tools.util.base.DiffViewerBase
import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import com.intellij.openapi.diagnostic.logger

private val LOG = logger<GiteaPRDiffExtension>()

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
        val discussionsVm = context.getUserData(GiteaPRDiscussionsViewModels.CONTEXT_KEY) ?: return
        val fileVm = request.getUserData(GiteaPRDiffFileViewModel.CONTEXT_KEY) ?: return

        val job = SupervisorJob()
        Disposer.register(viewer as Disposable, Disposable { job.cancel() })
        val cs = CoroutineScope(job + Dispatchers.Main)

        // showCodeReview installs the gutter-controls and inlay-rendering coroutines as siblings
        // in one plain (non-supervisor) coroutineScope internally — if the gutter install throws
        // on a not-yet-fully-initialized editor (most common on the very first diff viewer shown,
        // especially for a single-file/single-commit PR whose content resolves fastest), structured
        // concurrency cancels the inlay coroutine too and both silently drop, with no retry. Same
        // race launchReviewToolbar already works around for the sibling toolbar; each retry here
        // reconstructs the whole call fresh (confirmed safe against the platform source: each
        // gutter-controls install is a fresh, cleanly-disposed InstalledRenderer).
        cs.launchNow {
            var attempt = 0
            while (true) {
                try {
                    viewer.showCodeReview(
                        // The review-submit control itself is wired into the diff header via
                        // DiffUserDataKeys.CONTEXT_ACTIONS (GiteaPRDiffVirtualFile.createViewer) —
                        // this factory only wires gutter controls/inlays, mirroring the bundled
                        // GitHub plugin's split between GHPRReviewDiffExtension (gutter/inlays) and
                        // GHPRDiffService.createDiffContext (header toolbar).
                        modelFactory = { editor, side, locationToLine, lineToLocation, _ ->
                            GiteaPRDiffEditorModel(this, project, fileVm.file, side, discussionsVm, locationToLine, lineToLocation, editor)
                        },
                        rendererFactory = { inlayModel ->
                            GiteaPRInlayComponentsFactory.createRenderer(project, this, inlayModel, discussionsVm)
                        }
                    )
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    attempt++
                    if (attempt >= REVIEW_UI_INSTALL_RETRY_ATTEMPTS) {
                        LOG.warn("Failed to install PR review gutter controls/inlays after $attempt attempts", e)
                        return@launchNow
                    }
                    delay(REVIEW_UI_INSTALL_RETRY_DELAY_MS)
                }
            }
        }
    }
}
