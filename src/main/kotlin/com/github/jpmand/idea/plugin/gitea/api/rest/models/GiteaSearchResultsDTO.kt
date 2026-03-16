package com.github.jpmand.idea.plugin.gitea.api.rest.models

open class GiteaSearchResultsDTO(
  val data: Collection<GiteaRepositoryDTO>,
  val ok: Boolean
)