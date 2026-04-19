package com.github.jpmand.idea.plugin.gitea.pullrequest.diff

import com.github.jpmand.idea.plugin.gitea.api.rest.models.pr.GiteaPRFileStatusEnum
import com.github.jpmand.idea.plugin.gitea.api.rest.models.pr.GiteaPullRequestFileDTO

data class GiteaPRChangedFile(
    val filename: String,
    val previousFilename: String?,
    val sha: String?,
    val status: GiteaPRFileStatusEnum?,
    val additions: Int,
    val deletions: Int,
    val changes: Int,
    val patch: String?,
)

fun GiteaPullRequestFileDTO.toChangedFile() = GiteaPRChangedFile(
    filename = filename,
    previousFilename = previousFilename,
    sha = sha,
    status = status,
    additions = additions,
    deletions = deletions,
    changes = changes,
    patch = patch,
)
