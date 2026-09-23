package com.github.jpmand.idea.plugin.gitea.pullrequest

import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorPolicy
import com.intellij.openapi.fileEditor.FileEditorProvider
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

/** Supplies [GiteaPRTimelineFileEditor] for [GiteaPRTimelineVirtualFile]. Registered in plugin.xml. */
class GiteaPRTimelineEditorProvider : FileEditorProvider, DumbAware {

    override fun accept(project: Project, file: VirtualFile): Boolean = file is GiteaPRTimelineVirtualFile

    override fun createEditor(project: Project, file: VirtualFile): FileEditor =
        GiteaPRTimelineFileEditor(project, file as GiteaPRTimelineVirtualFile)

    override fun getEditorTypeId(): String = "GiteaPRTimelineEditor"

    override fun getPolicy(): FileEditorPolicy = FileEditorPolicy.HIDE_DEFAULT_EDITOR
}
