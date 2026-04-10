@file:Suppress("UnstableApiUsage")

package com.github.jpmand.idea.plugin.gitea.pullrequest.file

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.databind.introspect.VisibilityChecker
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.vcs.editor.ComplexPath
import com.intellij.vcs.editor.ComplexPathSerializer
import com.intellij.vcs.editor.ComplexPathVirtualFileSystem

/**
 * Virtual file system for Gitea PR diff virtual files.
 * Protocol: [PROTOCOL] ("gitea-pr")
 *
 * Register in plugin.xml as:
 * ```xml
 * <vfs.impl implementation="...GiteaPRVirtualFileSystem"/>
 * ```
 */
class GiteaPRVirtualFileSystem : ComplexPathVirtualFileSystem<GiteaPRVirtualFileSystem.FilePath>(PathSerializer()) {

    override fun getProtocol() = PROTOCOL

    override fun findOrCreateFile(project: Project, path: FilePath): VirtualFile? =
        GiteaPRDiffVirtualFile(
            connectionId = path.sessionId,
            projectPath = path.projectHash,
            prNumber = path.prNumber,
            baseSha = path.baseSha,
            headSha = path.headSha,
            relativePath = path.relativePath
        )

    fun getPath(
        connectionId: String,
        project: Project,
        prNumber: Int,
        baseSha: String,
        headSha: String,
        relativePath: String,
    ): String = getPath(FilePath(connectionId, project.locationHash, prNumber, baseSha, headSha, relativePath))

    fun getPath(
        connectionId: String,
        projectPath: String,
        prNumber: Int,
        baseSha: String,
        headSha: String,
        relativePath: String,
    ): String = getPath(FilePath(connectionId, projectPath, prNumber, baseSha, headSha, relativePath))

    data class FilePath(
        override val sessionId: String,
        override val projectHash: String,
        val prNumber: Int,
        val baseSha: String,
        val headSha: String,
        val relativePath: String,
    ) : ComplexPath

    private class PathSerializer : ComplexPathSerializer<FilePath> {
        private val mapper = jacksonObjectMapper().apply {
            setVisibility(
                VisibilityChecker.Std(
                    JsonAutoDetect.Visibility.NONE,
                    JsonAutoDetect.Visibility.NONE,
                    JsonAutoDetect.Visibility.NONE,
                    JsonAutoDetect.Visibility.NONE,
                    JsonAutoDetect.Visibility.ANY
                )
            )
        }

        override fun serialize(path: FilePath): String = mapper.writeValueAsString(path)
        override fun deserialize(rawPath: String): FilePath? =
            runCatching { mapper.readValue(rawPath, FilePath::class.java) }.getOrNull()
    }

    companion object {
        const val PROTOCOL = "gitea-pr"

        fun getInstance(): GiteaPRVirtualFileSystem =
            VirtualFileManager.getInstance().getFileSystem(PROTOCOL) as GiteaPRVirtualFileSystem
    }
}
