package com.github.jpmand.idea.plugin.gitea.api.rest.models.pr

import com.github.jpmand.idea.plugin.gitea.api.models.GiteaPullRequestComment
import com.github.jpmand.idea.plugin.gitea.api.rest.models.GiteaUserDTO
import java.util.Date

open class GiteaIssueCommentDTO(
    val id: Long,
    val user: GiteaUserDTO?,
    val body: String?,
    val createdAt: Date?,
    val updatedAt: Date?
) {
    fun toComment(): GiteaPullRequestComment = GiteaPullRequestComment(
        id = id,
        author = user?.toUser() ?: throw IllegalStateException("Issue comment user is null for comment $id"),
        body = body,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

