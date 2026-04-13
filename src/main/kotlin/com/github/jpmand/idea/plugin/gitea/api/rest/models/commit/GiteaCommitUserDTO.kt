package com.github.jpmand.idea.plugin.gitea.api.rest.models.commit

import java.util.Date

/**
 * CommitUser contains information of a user in the context of a commit.
 */
open class GiteaCommitUserDTO(
  val date: Date,
  val email: String,
  val name: String
)