package com.github.jpmand.idea.plugin.gitea.api.models

import com.github.jpmand.idea.plugin.gitea.api.rest.dto.CommitStatus

enum class GiteaCommitStatusState { PENDING, SUCCESS, ERROR, FAILURE, WARNING, SKIPPED, UNKNOWN }

data class GiteaCommitStatus(
    val context: String?,
    val description: String?,
    val status: GiteaCommitStatusState,
    val targetUrl: String?,
) {
    companion object {
        fun fromDto(dto: CommitStatus): GiteaCommitStatus = GiteaCommitStatus(
            context = dto.context,
            description = dto.description,
            status = when (dto.status) {
                CommitStatus.Status.PENDING -> GiteaCommitStatusState.PENDING
                CommitStatus.Status.SUCCESS -> GiteaCommitStatusState.SUCCESS
                CommitStatus.Status.ERROR -> GiteaCommitStatusState.ERROR
                CommitStatus.Status.FAILURE -> GiteaCommitStatusState.FAILURE
                CommitStatus.Status.WARNING -> GiteaCommitStatusState.WARNING
                CommitStatus.Status.SKIPPED -> GiteaCommitStatusState.SKIPPED
                null -> GiteaCommitStatusState.UNKNOWN
            },
            targetUrl = dto.targetUrl,
        )
    }
}
