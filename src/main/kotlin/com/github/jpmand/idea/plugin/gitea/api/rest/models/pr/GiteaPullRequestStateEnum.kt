package com.github.jpmand.idea.plugin.gitea.api.rest.models.pr

enum class GiteaPullRequestStateEnum {
  OPEN("open"),
  CLOSED("closed"),
  ALL("all");
  val value: String

  constructor(value: String) {
    this.value = value
  }
}