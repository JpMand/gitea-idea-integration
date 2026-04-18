package com.github.jpmand.idea.plugin.gitea.pullrequest.diff

import com.github.jpmand.idea.plugin.gitea.pullrequest.editor.GiteaPRDiffEditorModel
import com.github.jpmand.idea.plugin.gitea.pullrequest.editor.GiteaPRInlayComponentsFactory
import com.github.jpmand.idea.plugin.gitea.pullrequest.review.GiteaPRDiscussionsViewModels
import com.intellij.collaboration.async.launchNow
import com.intellij.collaboration.ui.codereview.diff.viewer.showCodeReview
import com.intellij.diff.DiffContext
import com.intellij.diff.DiffExtension
import com.intellij.diff.FrameDiffTool
import com.intellij.diff.requests.DiffRequest
import com.intellij.diff.tools.util.base.DiffViewerBase
import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
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
        val discussionsVm = context.getUserData(GiteaPRDiscussionsViewModels.CONTEXT_KEY) ?: return
        val fileVm = request.getUserData(GiteaPRDiffFileViewModel.CONTEXT_KEY) ?: return

        val job = SupervisorJob()
        Disposer.register(viewer as Disposable, Disposable { job.cancel() })
        val cs = CoroutineScope(job + Dispatchers.Main)

        cs.launchNow {
            viewer.showCodeReview(
                modelFactory = { _, side, locationToLine, lineToLocation, _ ->
                    GiteaPRDiffEditorModel(this, fileVm.file.filename, side, discussionsVm, locationToLine, lineToLocation)
                },
                rendererFactory = { inlayModel ->
                    GiteaPRInlayComponentsFactory.createRenderer(this, inlayModel)
                }
            )
        }
    }
}
