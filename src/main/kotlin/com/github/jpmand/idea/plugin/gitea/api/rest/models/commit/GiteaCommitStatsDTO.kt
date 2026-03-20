package com.github.jpmand.idea.plugin.gitea.api.rest.models.commit

open class GiteaCommitStatsDTO(
  val additions: Int,
  val deletions: Int,
  val total: Int
)