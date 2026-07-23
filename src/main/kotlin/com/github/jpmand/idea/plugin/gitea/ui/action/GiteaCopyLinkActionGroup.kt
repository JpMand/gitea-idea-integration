package com.github.jpmand.idea.plugin.gitea.ui.action

import com.github.jpmand.idea.plugin.gitea.GiteaRepositoriesManager
import com.intellij.openapi.components.service
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.Project
import git4idea.remote.hosting.HostedGitRepositoriesManager
import git4idea.remote.hosting.action.GlobalHostedGitRepositoryReferenceActionGroup
import git4idea.remote.hosting.action.HostedGitRepositoryReference
import java.awt.datatransfer.StringSelection
import java.net.URI

class GiteaCopyLinkActionGroup : GlobalHostedGitRepositoryReferenceActionGroup() {
    override fun repositoriesManager(project: Project): HostedGitRepositoriesManager<*> = project.service<GiteaRepositoriesManager>()

    override fun getUri(repository: URI, revisionHash: String): URI = GiteaURLUtil.getWebURI(repository, revisionHash)

    override fun getUri(repository: URI, revisionHash: String, relativePath: String, lineRange: IntRange?): URI = GiteaURLUtil.getWebURI(repository, revisionHash, relativePath, lineRange)

    override fun handleReference(reference: HostedGitRepositoryReference) {
        val uri = reference.buildWebURI()?.toString() ?: return
        CopyPasteManager.getInstance().setContents(StringSelection(uri))
    }
}