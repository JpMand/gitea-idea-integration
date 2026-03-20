package com.github.jpmand.idea.plugin.gitea.api.models

import java.util.Date

/**
 * Domain object representing a Gitea pull request.
 * Converted from [com.github.jpmand.idea.plugin.gitea.api.rest.models.pr.GiteaPullRequestDTO]
 * via [com.github.jpmand.idea.plugin.gitea.api.rest.models.pr.GiteaPullRequestDTO.toPullRequest].
 */
data class GiteaPullRequest(
    val number: Int,
    val title: String,
    val body: String?,
    val state: String?,
    val author: GiteaUser,
    val headBranch: String,
    val baseBranch: String,
    val htmlUrl: String,
    val createdAt: Date,
    val updatedAt: Date,
    val labels: List<GiteaLabel>,
    val assignees: List<GiteaUser>,
    val requestedReviewers: List<GiteaUser>,
    val reviewComments: Int,
    val merged: Boolean,
    val draft: Boolean
)

