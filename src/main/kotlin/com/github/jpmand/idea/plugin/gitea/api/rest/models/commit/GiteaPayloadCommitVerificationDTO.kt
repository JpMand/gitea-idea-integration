package com.github.jpmand.idea.plugin.gitea.api.rest.models.commit

import com.github.jpmand.idea.plugin.gitea.api.rest.models.GiteaPayloadUserDTO

open class GiteaPayloadCommitVerificationDTO(
  val payload: String,
  val reason: String,
  val signature: String,
  val signer: GiteaPayloadUserDTO,
  val verified: Boolean
)