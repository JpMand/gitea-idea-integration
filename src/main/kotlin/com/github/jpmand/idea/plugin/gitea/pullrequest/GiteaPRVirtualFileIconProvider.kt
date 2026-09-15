package com.github.jpmand.idea.plugin.gitea.pullrequest

import com.github.jpmand.idea.plugin.gitea.GiteaIcons
import com.intellij.ide.FileIconProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import javax.swing.Icon

/**
 * Gives the Timeline ("Conversation") editor tab the Gitea logo instead of the platform's default
 * plain-text file icon — the proper mechanism for this (registered `<fileIconProvider>`, same as
 * the bundled GitHub plugin's `GHPRVirtualFileIconProvider`), rather than a fake [com.intellij.openapi.fileTypes.FileType]
 * (which risks being consulted anywhere a real file type would be — associations, scopes, etc.).
 */
class GiteaPRVirtualFileIconProvider : FileIconProvider {
    override fun getIcon(file: VirtualFile, flags: Int, project: Project?): Icon? =
        if (file is GiteaPRTimelineVirtualFile) GiteaIcons.Logo else null
}
