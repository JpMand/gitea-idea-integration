package com.github.jpmand.idea.plugin.gitea.ui.action

import com.github.jpmand.idea.plugin.gitea.GiteaIcons
import com.github.jpmand.idea.plugin.gitea.GiteaRepositoriesManager
import com.github.jpmand.idea.plugin.gitea.util.GiteaBundle
import com.intellij.collaboration.util.resolveRelative
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.util.io.URLUtil
import com.intellij.util.withFragment
import git4idea.remote.hosting.HostedGitRepositoriesManager
import git4idea.remote.hosting.action.GlobalHostedGitRepositoryReferenceActionGroup
import git4idea.remote.hosting.action.HostedGitRepositoryReference
import java.net.URI

class GiteaOpenInBrowserActionGroup : GlobalHostedGitRepositoryReferenceActionGroup(
    GiteaBundle.messagePointer("open.on.gitea.action"),
    GiteaBundle.messagePointer("open.on.gitea.action.description"),
    { GiteaIcons.Logo }
) {
    override fun repositoriesManager(project: Project): HostedGitRepositoriesManager<*> = project.service<GiteaRepositoriesManager>()

    override fun getUri(repository: URI, revisionHash: String): URI = GiteaURLUtil.getWebURI(repository, revisionHash)

    override fun getUri(repository: URI, revisionHash: String, relativePath: String, lineRange: IntRange?): URI {
        return GiteaURLUtil.getWebURI(repository, revisionHash, relativePath, lineRange)
    }

    override fun handleReference(reference: HostedGitRepositoryReference) {
        val uri = reference.buildWebURI() ?: return
        BrowserUtil.browse(uri)
    }
}

object GiteaURLUtil {

    fun getWebURI(repository: URI, revision: String): URI =
        repository.resolveRelative("src/commit").resolveRelative(revision)

    fun getWebURI(repository: URI, revision: String, relativePath: String, lineRange: IntRange?): URI {
        val fileUri = repository.resolveRelative("src/commit").resolveRelative(revision)
            .resolveRelative(URLUtil.encodePath(relativePath))
        return if (lineRange != null) {
            val fragmentBuilder = StringBuilder()
            fragmentBuilder.append("L").append(lineRange.first + 1)
            if (lineRange.last != lineRange.first) {
                fragmentBuilder.append("-L").append(lineRange.last + 1)
            }
            fileUri.withFragment(fragmentBuilder.toString())
        }
        else {
            fileUri
        }
    }
}