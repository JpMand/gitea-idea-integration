package com.github.jpmand.idea.plugin.gitea.api.rest.models.commit

import com.github.jpmand.idea.plugin.gitea.api.rest.models.GiteaUserDTO
import java.util.Date

open class GiteaCommitDTO(
  val committer: GiteaUserDTO,
  val created: Date,
  val author: GiteaUserDTO?,
  val commit: GiteaRepoCommitDTO,
  val files: Collection<GiteaCommitAffectedFilesDTO>,
  val htmlUrl: String,
  val parents: Collection<GiteaCommitMetaDTO>,
  val sha: String,
  val stats: Collection<GiteaCommitStatsDTO>,
  val url: String
)