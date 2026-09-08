package com.github.jpmand.idea.plugin.gitea.api.models

import com.github.jpmand.idea.plugin.gitea.api.rest.dto.User
import com.intellij.collaboration.auth.AccountDetails
import com.intellij.collaboration.ui.codereview.user.CodeReviewUser
import com.intellij.openapi.util.NlsSafe

open class GiteaUser(
  val id: Long,
  val login: @NlsSafe String,
  override val avatarUrl: String?,
  val email: @NlsSafe String?,
  val fullName: @NlsSafe String?,
  val htmlUrl: String?
) : AccountDetails, CodeReviewUser {

  override val name: String
    get() = login

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is GiteaUser) return false

    if (id != other.id) return false
    if (login != other.login) return false
    if (avatarUrl != other.avatarUrl) return false

    return true
  }

  override fun hashCode(): Int {
    var result = id.hashCode()
    result = 31 * result + login.hashCode()
    result = 31 * result + (avatarUrl?.hashCode() ?: 0)
    return result
  }

  companion object {
    fun fromDto(dto: User): GiteaUser = GiteaUser(
      id = dto.id ?: 0L,
      login = dto.login ?: "",
      avatarUrl = dto.avatarUrl,
      email = dto.email,
      // Gitea returns "" (not null) for users without a display name; collapse it so callers
      // that fall back `fullName ?: login` (e.g. the platform's UserPresentation) don't render blank.
      fullName = dto.fullName?.takeIf { it.isNotBlank() },
      htmlUrl = dto.htmlUrl,
    )
  }
}