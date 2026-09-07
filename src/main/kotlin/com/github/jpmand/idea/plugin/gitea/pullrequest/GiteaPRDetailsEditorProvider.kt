package com.github.jpmand.idea.plugin.gitea.pullrequest

import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorPolicy
import com.intellij.openapi.fileEditor.FileEditorProvider
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

/**
 * Supplies [GiteaPRDetailsFileEditor] for [GiteaPRDetailsVirtualFile]. Registered in plugin.xml
 * as a `fileEditorProvider` — unlike the diff view (handled generically by the platform's own
 * `DiffViewerVirtualFile` machinery), a plain custom virtual file needs an explicit provider.
 */
class GiteaPRDetailsEditorProvider : FileEditorProvider, DumbAware {

    override fun accept(project: Project, file: VirtualFile): Boolean = file is GiteaPRDetailsVirtualFile

    override fun createEditor(project: Project, file: VirtualFile): FileEditor {
        val detailsFile = file as GiteaPRDetailsVirtualFile
        return GiteaPRDetailsFileEditor(project, detailsFile, detailsFile.repository, detailsFile.pr)
    }

    override fun getEditorTypeId(): String = "GiteaPRDetailsEditor"

    override fun getPolicy(): FileEditorPolicy = FileEditorPolicy.HIDE_DEFAULT_EDITOR
}
