package com.github.jpmand.idea.plugin.gitea.pullrequest.diff

import com.github.jpmand.idea.plugin.gitea.api.rest.dto.ChangedFile
import com.github.jpmand.idea.plugin.gitea.api.rest.dto.CommitAffectedFiles
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
    status = status.toFileStatus(),
    additions = additions?.toInt() ?: 0,
    deletions = deletions?.toInt() ?: 0,
    changes = changes?.toInt() ?: 0,
)

/** For a single-commit view: the Gitea commit endpoint only carries filename + status. */
fun CommitAffectedFiles.toChangedFile() = GiteaPRChangedFile(
    filename = filename.orEmpty(),
    previousFilename = null,
    status = status.toFileStatus(),
    additions = 0,
    deletions = 0,
    changes = 0,
)

private fun String?.toFileStatus(): GiteaPRFileStatusEnum? =
    this?.let { s -> GiteaPRFileStatusEnum.entries.firstOrNull { it.name.equals(s, ignoreCase = true) } }
