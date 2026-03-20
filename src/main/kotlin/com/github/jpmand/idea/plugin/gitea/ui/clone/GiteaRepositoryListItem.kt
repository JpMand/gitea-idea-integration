package com.github.jpmand.idea.plugin.gitea.ui.clone

import com.github.jpmand.idea.plugin.gitea.api.models.GiteaUser
import com.github.jpmand.idea.plugin.gitea.api.rest.models.GiteaRepositoryDTO
import com.github.jpmand.idea.plugin.gitea.authentication.account.GiteaAccount

sealed class GiteaRepositoryListItem(
  val account: GiteaAccount
) {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as GiteaRepositoryListItem
    return account == other.account
  }

  override fun hashCode(): Int {
    return account.hashCode()
  }

  class Repository(
    account: GiteaAccount,
    val user: GiteaUser,
    val repository: GiteaRepositoryDTO
  ) : GiteaRepositoryListItem(account) {

    override fun equals(other: Any?): Boolean {
      if (this === other) return true
      if (javaClass != other?.javaClass) return false
      if (!super.equals(other)) return false

      other as Repository

      if (user != other.user) return false
      if (repository != other.repository) return false

      return true
    }

    override fun hashCode(): Int {
      var result = super.hashCode()
      result = 31 * result + user.hashCode()
      result = 31 * result + repository.hashCode()
      return result
    }
  }

  class Error(
    account: GiteaAccount,
    val error: Throwable
  ) : GiteaRepositoryListItem(account) {

    override fun equals(other: Any?): Boolean {
      if (this === other) return true
      if (javaClass != other?.javaClass) return false
      if (!super.equals(other)) return false

      other as Error

      return error == other.error
    }

    override fun hashCode(): Int {
      var result = super.hashCode()
      result = 31 * result + error.hashCode()
      return result
    }
  }
}