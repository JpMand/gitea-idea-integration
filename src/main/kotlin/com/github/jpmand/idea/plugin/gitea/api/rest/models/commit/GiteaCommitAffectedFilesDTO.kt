package com.github.jpmand.idea.plugin.gitea.api.rest.models.commit

open class GiteaCommitAffectedFilesDTO(
  val filename: String,
  val status: String,
)