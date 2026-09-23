package com.github.jpmand.idea.plugin.gitea.ui.action

import com.github.jpmand.idea.plugin.gitea.GiteaIcons
import com.github.jpmand.idea.plugin.gitea.GiteaRepositoriesManager
import com.github.jpmand.idea.plugin.gitea.util.GiteaBundle
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.components.service
import com.intellij.openapi.vcs.actions.ShowAnnotateOperationsPopup
import com.intellij.openapi.vcs.annotate.FileAnnotation
import com.intellij.util.asSafely
import git4idea.GitRevisionNumber
import git4idea.annotate.GitFileAnnotation
import git4idea.remote.hosting.action.HostedGitRepositoryReference
import git4idea.remote.hosting.action.HostedGitRepositoryReferenceActionGroup
import git4idea.remote.hosting.action.HostedGitRepositoryReferenceUtil

internal class GiteaOpenInBrowserFromAnnotationActionGroup(val annotation: FileAnnotation) :
    HostedGitRepositoryReferenceActionGroup(
        GiteaBundle.messagePointer("open.on.gitea.action"),
        GiteaBundle.messagePointer("open.on.gitea.action.description"),
        { GiteaIcons.Logo },
    ) {

    override fun findReferences(dataContext: DataContext): List<HostedGitRepositoryReference> {
        if (annotation !is GitFileAnnotation) return emptyList()
        val project = annotation.project
        val virtualFile = annotation.file

        val revision = ShowAnnotateOperationsPopup.getAnnotationLineNumber(dataContext).takeIf { it >= 0 }
            ?.let { annotation.getLineRevisionNumber(it) }
            ?.asSafely<GitRevisionNumber>() ?: return emptyList()

        return HostedGitRepositoryReferenceUtil.findReferences(
            project,
            project.service<GiteaRepositoriesManager>(),
            virtualFile,
            revision,
            GiteaURLUtil::getWebURI
        )
    }

    override fun handleReference(reference: HostedGitRepositoryReference) {
        val uri = reference.buildWebURI() ?: return
        BrowserUtil.browse(uri)
    }
}