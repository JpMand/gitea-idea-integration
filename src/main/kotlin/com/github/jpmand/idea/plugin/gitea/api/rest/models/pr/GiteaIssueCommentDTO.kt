package com.github.jpmand.idea.plugin.gitea.api.rest.models.pr

import com.github.jpmand.idea.plugin.gitea.api.rest.models.GiteaUserDTO
import java.util.Date

open class GiteaIssueCommentDTO(
    val id: Long,
    val user: GiteaUserDTO?,
    val body: String?,
    val createdAt: Date?,
    val updatedAt: Date?
)

