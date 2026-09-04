package com.github.jpmand.idea.plugin.gitea.api.models

import com.github.jpmand.idea.plugin.gitea.api.rest.dto.PRBranchInfo
import com.github.jpmand.idea.plugin.gitea.api.rest.dto.PullRequest
import java.util.*

class GiteaPullRequest(
    val id: Long,
    val number: Long,
    val title: String,
    val body: String?,
    val state: String?,
    val draft: Boolean,
    val merged: Boolean,
    val mergeable: Boolean,
    val author: GiteaUser,
    val assignee: GiteaUser?,
    val assignees: List<GiteaUser>,
    val labels: List<GiteaLabel>,
    val base: GiteaBranchInfo,
    val head: GiteaBranchInfo,
    val mergeCommitSha: String?,
    val htmlUrl: String,
    val createdAt: Date,
    val updatedAt: Date,
    val mergedAt: Date?,
    val closedAt: Date?,
    val reviewComments: Int,
    val changedFiles: Int?,
    val additions: Int?,
    val deletions: Int?,
    val requestedReviewers: List<GiteaUser>
){
    companion object{
        fun fromDto(dto : PullRequest): GiteaPullRequest {
            return GiteaPullRequest(
                id = dto.id ?: 0,
                number = dto.number ?: 0,
                title = dto.title ?: "",
                body = dto.body,
                state = dto.state?.value,
                draft = dto.draft ?: false,
                merged = dto.merged ?: false,
                mergeable = dto.mergeable ?: false,
                author = dto.user?.let { GiteaUser.fromDto(it) } ?: UNKNOWN_USER,
                assignee = dto.assignee?.let { GiteaUser.fromDto(it) },
                assignees = dto.assignees?.map { GiteaUser.fromDto(it) } ?: emptyList(),
                labels = dto.labels?.map { GiteaLabel.fromDto(it) } ?: emptyList(),
                base = GiteaBranchInfo.fromDto(dto.base?: PRBranchInfo()),
                head = GiteaBranchInfo.fromDto(dto.head?: PRBranchInfo()),
                mergeCommitSha = dto.mergeCommitSha,
                htmlUrl = dto.htmlUrl ?: "",
                createdAt = dto.createdAt?.toDate() ?: Date(),
                updatedAt = dto.updatedAt?.toDate() ?: Date(),
                mergedAt = dto.mergedAt?.toDate(),
                closedAt = dto.closedAt?.toDate(),
                reviewComments = dto.reviewComments?.toInt() ?: 0,
                changedFiles = dto.changedFiles?.toInt(),
                additions = dto.additions?.toInt(),
                deletions = dto.deletions?.toInt(),
                requestedReviewers = dto.requestedReviewers?.map { GiteaUser.fromDto(it) } ?: emptyList()
            )
        }

        private val UNKNOWN_USER = GiteaUser(
            id = 0L,
            login = "",
            avatarUrl = null,
            email = null,
            fullName = null,
            htmlUrl = null,
        )
    }
}
