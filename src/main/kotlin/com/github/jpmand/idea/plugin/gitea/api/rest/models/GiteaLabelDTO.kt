package com.github.jpmand.idea.plugin.gitea.api.rest.models

open class GiteaLabelDTO(
  val color: String,
  val description: String,
  val id: Int,
  val name: String,
  val url: String
)
