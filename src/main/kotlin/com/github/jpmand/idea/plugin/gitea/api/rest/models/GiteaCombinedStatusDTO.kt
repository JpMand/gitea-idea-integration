package com.github.jpmand.idea.plugin.gitea.api.rest.models

/** DTO for the combined CI status of a commit (CombinedStatus in the Gitea Swagger spec). */
class GiteaCombinedStatusDTO(
    val sha: String?,
    /** Aggregate state: pending, success, error, failure, warning, skipped. */
    val state: String?,
    val statuses: List<GiteaCommitStatusDTO>?,
    val totalCount: Long?,
    val commitUrl: String?,
    val url: String?,
)
