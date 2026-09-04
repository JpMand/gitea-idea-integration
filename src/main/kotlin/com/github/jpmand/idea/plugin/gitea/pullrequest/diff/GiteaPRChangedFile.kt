package com.github.jpmand.idea.plugin.gitea.pullrequest.diff

import com.github.jpmand.idea.plugin.gitea.api.rest.dto.ChangedFile
import com.github.jpmand.idea.plugin.gitea.api.rest.pr.GiteaPRFileStatusEnum

data class GiteaPRChangedFile(
    val filename: String,
    val previousFilename: String?,
    val status: GiteaPRFileStatusEnum?,
    val additions: Int,
    val deletions: Int,
    val changes: Int,
)

fun ChangedFile.toChangedFile() = GiteaPRChangedFile(
    filename = filename.orEmpty(),
    previousFilename = previousFilename,
    status = status?.let { s -> GiteaPRFileStatusEnum.entries.firstOrNull { it.name.equals(s, ignoreCase = true) } },
    additions = additions?.toInt() ?: 0,
    deletions = deletions?.toInt() ?: 0,
    changes = changes?.toInt() ?: 0,
)
