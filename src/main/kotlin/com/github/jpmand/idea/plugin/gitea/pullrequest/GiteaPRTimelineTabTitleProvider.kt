package com.github.jpmand.idea.plugin.gitea.pullrequest

import com.intellij.openapi.fileEditor.impl.EditorTabTitleProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.HtmlChunk
import com.intellij.openapi.vfs.VirtualFile

/**
 * Title and tooltip of the Timeline ("Conversation") editor tab. Without a provider the platform
 * uses the file's path as the tooltip, which for a [com.intellij.testFramework.LightVirtualFile]
 * is `/` + its name, shown as `\…` on Windows; and the tooltip is where the full PR title is
 * shown once [GiteaPRTimelineVirtualFile.getPresentableName] has shortened it for the tab.
 */
class GiteaPRTimelineTabTitleProvider : EditorTabTitleProvider {
    override fun getEditorTabTitle(project: Project, file: VirtualFile): String? =
        (file as? GiteaPRTimelineVirtualFile)?.presentableName

    // As text, so a PR title with `<…>` in it is shown, not rendered.
    override fun getEditorTabTooltipHtml(project: Project, virtualFile: VirtualFile): HtmlChunk? =
        (virtualFile as? GiteaPRTimelineVirtualFile)?.let { HtmlChunk.text(it.fullPresentableName) }
}
