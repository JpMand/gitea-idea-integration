package com.github.jpmand.idea.plugin.gitea.api.rest.models.pr

import com.github.jpmand.idea.plugin.gitea.api.rest.models.GiteaRepositoryDTO

open class GiteaPRBranchInfoDTO(
  val label: String,
  val ref: String,
  val repo: GiteaRepositoryDTO,
  val repoId: Int,
  val sha: String
)