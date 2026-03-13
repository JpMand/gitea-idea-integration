package com.github.jpmand.idea.plugin.gitea.api.rest.models

import com.fasterxml.jackson.annotation.JsonProperty
import com.github.jpmand.idea.plugin.gitea.api.models.GiteaUser
import java.util.Date

open class GiteaUserDTO(
  val id: Int = 0,
  val login: String = "",
  @param:JsonProperty("avatar_url") val avatarUrl: String? = null,
  val email: String? = null,
  @param:JsonProperty("full_name") val fullName: String? = null,
  @param:JsonProperty("html_url") val htmlUrl: String? = null,
  @param:JsonProperty("last_login") val lastLogin: Date? = null
) {
  fun toUser(): GiteaUser =
    GiteaUser(id, login, avatarUrl, email, fullName, htmlUrl, lastLogin)
}
