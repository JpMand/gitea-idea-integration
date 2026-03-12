package com.github.jpmand.idea.plugin.gitea.api.models

import com.intellij.collaboration.auth.AccountDetails
import com.intellij.collaboration.ui.codereview.user.CodeReviewUser
import com.intellij.openapi.util.NlsSafe
import kotlinx.datetime.LocalDateTime

open class GiteaUser(
  val id: Int,
  val login: @NlsSafe String,
  override val avatarUrl: String?,
  val email: @NlsSafe String?,
  val fullName: @NlsSafe String?,
  val htmlUrl : String?,
  val lastLogin: LocalDateTime?
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
    var result = id
    result = 31 * result + login.hashCode()
    result = 31 * result + (avatarUrl?.hashCode() ?: 0)
    return result
  }
}