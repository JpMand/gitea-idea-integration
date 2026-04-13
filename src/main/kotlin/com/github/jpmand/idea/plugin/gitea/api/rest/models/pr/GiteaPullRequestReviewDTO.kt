package com.github.jpmand.idea.plugin.gitea.api.rest.models.pr
import com.github.jpmand.idea.plugin.gitea.api.rest.models.GiteaUserDTO
import java.util.Date
open class GiteaPullRequestReviewDTO(
    val id: Long,
    val user: GiteaUserDTO?,
    val body: String?,
    val state: GiteaReviewStateEnum?,
    val submittedAt: Date?,
    val stale: Boolean,
    val official: Boolean,
    val dismissed: Boolean,
    val commitId: String?
)