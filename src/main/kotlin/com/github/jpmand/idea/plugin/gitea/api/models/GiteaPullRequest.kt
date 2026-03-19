package com.github.jpmand.idea.plugin.gitea.api.models

/**
 * Domain model for a Gitea Pull Request
 * Contains only the essential fields needed for the tool window display
 */
data class GiteaPullRequest(
  val id: Int,
  val number: Int,
  val title: String,
  val state: String?,
  val htmlUrl: String
)
