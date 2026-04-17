package com.github.jpmand.idea.plugin.gitea.api.models

import java.util.Date

data class GiteaPullRequest(
    val id: Int,
    val number: Int,
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
    val mergeBaseSha: String?,
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
)
