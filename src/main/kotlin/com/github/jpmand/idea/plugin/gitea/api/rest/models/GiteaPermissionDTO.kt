package com.github.jpmand.idea.plugin.gitea.api.rest.models

open class GiteaPermissionDTO(
  val admin: Boolean,
  val pull: Boolean,
  val push: Boolean
)