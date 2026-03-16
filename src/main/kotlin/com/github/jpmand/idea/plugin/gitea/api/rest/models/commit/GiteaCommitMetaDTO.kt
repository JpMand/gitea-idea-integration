package com.github.jpmand.idea.plugin.gitea.api.rest.models.commit

import java.util.Date

open class GiteaCommitMetaDTO(
  val created: Date?,
  val sha: String,
  val url: String
)