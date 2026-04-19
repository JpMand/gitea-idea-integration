package com.github.jpmand.idea.plugin.gitea.api.rest.models

/** DTO for a single commit status (CommitStatus in the Gitea Swagger spec). */
class GiteaCommitStatusDTO(
    val id: Long,
    /** Context label identifying the CI system (e.g. "ci/jenkins"). */
    val context: String?,
    val description: String?,
    /** State: pending, success, error, failure, warning, skipped. */
    val status: String?,
    val targetUrl: String?,
    val creator: GiteaUserDTO?,
    val createdAt: String?,
    val updatedAt: String?,
    val url: String?,
)
