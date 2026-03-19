package com.github.jpmand.idea.plugin.gitea.api.models

/**
 * Domain object representing a file changed in a pull request.
 * Converted from [com.github.jpmand.idea.plugin.gitea.api.rest.models.pr.GiteaPullRequestFileDTO]
 * via [com.github.jpmand.idea.plugin.gitea.api.rest.models.pr.GiteaPullRequestFileDTO.toChangedFile].
 */
data class GiteaChangedFile(
    val filename: String,
    val status: GiteaFileStatus,
    val additions: Int,
    val deletions: Int,
    val changes: Int,
    val patch: String?
)

