package com.github.jpmand.idea.plugin.gitea.api.rest.models.pr

import com.github.jpmand.idea.plugin.gitea.api.rest.models.GiteaLabelDTO
import com.github.jpmand.idea.plugin.gitea.api.rest.models.GiteaTeamDTO
import com.github.jpmand.idea.plugin.gitea.api.rest.models.GiteaUserDTO
import java.util.Date

open class GiteaPullRequestDTO(
  val additions: Int?,
  val allowMaintainerEdit: Boolean,
  val assignee: GiteaUserDTO?,
  val assignees: List<GiteaUserDTO>?,
  val base: GiteaPRBranchInfoDTO,
  val body: String?,
  val changedFiles: Int?,
  val closedAt: Date?,
  val comments: Int,
  val createdAt: Date,
  val deletions: Int?,
  val diffUrl: String,
  val draft: Boolean,
  val dueDate: Date?,
  val head: GiteaPRBranchInfoDTO,
  val htmlUrl: String,
  val id: Int,
  val isLocked: Boolean,
  val labels: List<GiteaLabelDTO>,
  val mergeBase: String?,
  val mergeCommitSha: String?,
  val mergeable: Boolean,
  val merged: Boolean,
  val mergedAt: Date?,
  val mergedBy: GiteaUserDTO?,
  val milestone: GiteaMilestoneDTO?,
  val number: Int,
  val patchUrl: String,
  val pinOrder: Int,
  val requestedReviewers: List<GiteaUserDTO>?,
  val requestedReviewersTeams: List<GiteaTeamDTO>?,
  val reviewComments: Int,
  val state: String?,
  val title: String,
  val updatedAt: Date,
  val url: String,
  val user: GiteaUserDTO
)
