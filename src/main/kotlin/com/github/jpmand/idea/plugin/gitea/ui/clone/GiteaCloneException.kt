package com.github.jpmand.idea.plugin.gitea.ui.clone

import com.github.jpmand.idea.plugin.gitea.authentication.account.GiteaAccount
import org.jetbrains.annotations.Nls

internal sealed interface GiteaCloneException {
  val account: GiteaAccount

  data class MissingAccessToken(override val account: GiteaAccount) : GiteaCloneException
  data class RevokedToken(override val account: GiteaAccount) : GiteaCloneException
  data class ConnectionError(override val account: GiteaAccount) : GiteaCloneException
  data class Unknown(override val account: GiteaAccount, val message: @Nls String) : GiteaCloneException
}