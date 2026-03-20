package com.github.jpmand.idea.plugin.gitea.api.rest.models.pr

import com.github.jpmand.idea.plugin.gitea.api.models.GiteaChangedFile
import com.github.jpmand.idea.plugin.gitea.api.models.GiteaFileStatus

open class GiteaPullRequestFileDTO(
    val sha: String?,
    val filename: String,
    val status: GiteaPRFileStatusEnum?,
    val additions: Int,
    val deletions: Int,
    val changes: Int,
    val rawUrl: String?,
    val contentsUrl: String?,
    val patch: String?
) {
    fun toChangedFile(): GiteaChangedFile = GiteaChangedFile(
        filename = filename,
        status = status?.toDomainStatus() ?: GiteaFileStatus.UNKNOWN,
        additions = additions,
        deletions = deletions,
        changes = changes,
        patch = patch
    )
}

private fun GiteaPRFileStatusEnum.toDomainStatus(): GiteaFileStatus = when (this) {
    GiteaPRFileStatusEnum.added -> GiteaFileStatus.ADDED
    GiteaPRFileStatusEnum.modified -> GiteaFileStatus.MODIFIED
    GiteaPRFileStatusEnum.deleted -> GiteaFileStatus.DELETED
    GiteaPRFileStatusEnum.renamed -> GiteaFileStatus.RENAMED
    GiteaPRFileStatusEnum.copied -> GiteaFileStatus.COPIED
    GiteaPRFileStatusEnum.changed -> GiteaFileStatus.CHANGED
    GiteaPRFileStatusEnum.unchanged -> GiteaFileStatus.UNCHANGED
}

