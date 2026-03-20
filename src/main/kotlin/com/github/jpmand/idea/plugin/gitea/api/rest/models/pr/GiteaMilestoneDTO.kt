package com.github.jpmand.idea.plugin.gitea.api.rest.models.pr

import com.github.jpmand.idea.plugin.gitea.api.rest.models.GiteaStateEnum
import java.util.*

open class GiteaMilestoneDTO(
  val id: Int,
  val title: String,
  val createdAt: Date,
  val updatedAt: Date?,
  val description: String?,
  val dueOn: Date?,
  val openIssues: Int?,
  val closedAt: Date?,
  val closedIssues: Int?,
  val state: GiteaStateEnum?
)