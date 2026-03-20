package com.github.jpmand.idea.plugin.gitea.api.rest.models

open class GiteaOrganizationDTO(
  val id: Int,
  val name: String,
  val avatarUrl: String?,
  val description: String?,
  val email: String?,
  val fullName: String?,
  val location: String?,
  val repoAdminChangeTeamAccess: Boolean?,
  val username: String?,
  val visibility: GiteaVisibilityEnum?,
  val website: String?
)