package com.github.jpmand.idea.plugin.gitea.api.rest.models.pr

open class GiteaPullRequestFileDTO(
    val sha: String?,
    val filename: String,
    val previousFilename: String?,
    val status: GiteaPRFileStatusEnum?,
    val additions: Int,
    val deletions: Int,
    val changes: Int,
    val rawUrl: String?,
    val contentsUrl: String?,
    val patch: String?
)