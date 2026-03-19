package com.github.jpmand.idea.plugin.gitea.api.rest.models

import com.github.jpmand.idea.plugin.gitea.api.models.GiteaLabel

open class GiteaLabelDTO(
  val color: String,
  val description: String,
  val id: Int,
  val name: String,
  val url: String
) {
  fun toLabel(): GiteaLabel = GiteaLabel(id, name, color, description)
}
