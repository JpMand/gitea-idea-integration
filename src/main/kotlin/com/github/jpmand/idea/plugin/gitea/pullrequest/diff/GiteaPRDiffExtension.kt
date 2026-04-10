@file:Suppress("UnstableApiUsage")

package com.github.jpmand.idea.plugin.gitea.pullrequest.diff

import com.intellij.diff.DiffContext
import com.intellij.diff.DiffExtension
import com.intellij.diff.FrameDiffTool.DiffViewer

/**
 * Entry point for hooking into the diff viewer lifecycle.
 *
 * Currently a minimal stub — future phases will attach inline comment inlays
 * by checking for [GiteaPRDiffViewModel.KEY] in the context and calling
 * `viewer.showCodeReview(...)`.
 *
 * Register in plugin.xml:
 * ```xml
 * <diff.DiffExtension implementation="...GiteaPRDiffExtension"/>
 * ```
 */
class GiteaPRDiffExtension : DiffExtension() {

    override fun onViewerCreated(viewer: DiffViewer, context: DiffContext, request: com.intellij.diff.requests.DiffRequest) {
        // Phase 4 (inlay system) will wire up code review inlays here.
        // For now, detect when a Gitea PR diff viewer is created and log for debugging.
        context.getUserData(GiteaPRDiffViewModel.KEY) ?: return
        // TODO Phase 4: viewer.showCodeReview(model) { inlayModel -> createInlayRenderer(inlayModel) }
    }
}
