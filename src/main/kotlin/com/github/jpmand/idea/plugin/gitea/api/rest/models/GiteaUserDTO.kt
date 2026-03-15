package com.github.jpmand.idea.plugin.gitea.api.rest.models

import com.fasterxml.jackson.annotation.JsonProperty
import com.github.jpmand.idea.plugin.gitea.api.models.GiteaUser
import java.util.Date

open class GiteaUserDTO(
  val id: Int,
  val login: String,
  val avatarUrl: String?,
  val email: String?,
  val fullName: String?,
  val htmlUrl: String?,
  val username: String?,
  val lastLogin: Date?
) {
  fun toUser(): GiteaUser =
    GiteaUser(id, login, avatarUrl, email, fullName, htmlUrl)

  override fun toString(): String {
    return "GiteaUserDTO(id=$id, login='$login', avatarUrl=$avatarUrl, email=$email, fullName=$fullName, htmlUrl=$htmlUrl, username=$username, lastLogin=$lastLogin)"
  }
}
