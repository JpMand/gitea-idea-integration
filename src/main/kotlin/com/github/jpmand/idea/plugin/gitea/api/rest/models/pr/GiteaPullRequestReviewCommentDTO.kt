package com.github.jpmand.idea.plugin.gitea.api.rest.models.pr

import com.github.jpmand.idea.plugin.gitea.api.rest.models.GiteaUserDTO
import java.util.Date

open class GiteaPullRequestReviewCommentDTO(
    val id: Long,
    val user: GiteaUserDTO?,
    val body: String?,
    val createdAt: Date?,
    val updatedAt: Date?,
    val path: String?,
    val diffHunk: String?,
    val originalPosition: Int?,
    val position: Int?,
    val commitId: String?,
    val originalCommitId: String?
)

