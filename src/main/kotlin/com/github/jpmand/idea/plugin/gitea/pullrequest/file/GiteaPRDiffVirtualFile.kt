@file:Suppress("UnstableApiUsage")

package com.github.jpmand.idea.plugin.gitea.pullrequest.file

import com.intellij.collaboration.file.codereview.CodeReviewDiffVirtualFile
import com.intellij.diff.editor.DiffEditorViewer
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.vfs.VirtualFileSystem

/**
 * A virtual file representing all changed files in a single Gitea PR diff.
 * Extends [CodeReviewDiffVirtualFile] so JetBrains diff infrastructure handles it.
 *
 * One instance per (PR, project). Identified by [connectionId] + [prNumber].
 * [createViewer] delegates to [GiteaPRDiffService] to produce a full PR [com.intellij.diff.impl.DiffRequestProcessor].
 */
class GiteaPRDiffVirtualFile(
    override val connectionId: String,
    val projectPath: String,
    val prNumber: Int,
    val baseSha: String,
    val headSha: String,
    /** Relative path of the file to show initially (may be empty for "show first file"). */
    val relativePath: String,
) : CodeReviewDiffVirtualFile("PR #$prNumber — ${relativePath.substringAfterLast('/')}") {

    override fun getPresentablePath(): String = relativePath.ifEmpty { "PR #$prNumber" }

    override fun getPresentableName(): String = if (relativePath.isNotEmpty())
        relativePath.substringAfterLast('/')
    else
        "PR #$prNumber"

    override fun isValid(): Boolean = findProject() != null

    override fun getFileSystem(): VirtualFileSystem = GiteaPRVirtualFileSystem.getInstance()

    override fun getPath(): String =
        GiteaPRVirtualFileSystem.getInstance()
            .getPath(connectionId, projectPath, prNumber, baseSha, headSha, relativePath)

    override fun createViewer(project: Project): DiffEditorViewer =
        project.service<GiteaPRDiffService>().createDiffRequestProcessor(prNumber, baseSha, headSha, relativePath)

    private fun findProject(): Project? =
        ProjectManager.getInstance().openProjects.firstOrNull { it.locationHash == projectPath }
}
