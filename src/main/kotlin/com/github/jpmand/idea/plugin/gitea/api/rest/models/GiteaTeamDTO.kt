package com.github.jpmand.idea.plugin.gitea.api.rest.models

open class GiteaTeamDTO(
  val id: Int,
  val name: String,
  val description: String?,
  val canCreateOrgRepo: Boolean?,
  val includesAllRepositories: Boolean?,
  val organization: GiteaOrganizationDTO?,
  val permission: GiteaPermissionDTO?,
  val units: List<String>?,
  val unitsMap: Map<String, String>?,
)