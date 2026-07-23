package com.github.jpmand.idea.plugin.gitea.extensions

import com.github.jpmand.idea.plugin.gitea.ui.action.GiteaOpenInBrowserFromAnnotationActionGroup
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.vcs.annotate.AnnotationGutterActionProvider
import com.intellij.openapi.vcs.annotate.FileAnnotation

class GiteaAnnotationGutterActionProvider : AnnotationGutterActionProvider {
    override fun createAction(p0: FileAnnotation): AnAction = GiteaOpenInBrowserFromAnnotationActionGroup(p0)
}