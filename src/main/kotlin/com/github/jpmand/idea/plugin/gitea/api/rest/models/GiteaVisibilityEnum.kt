package com.github.jpmand.idea.plugin.gitea.api.rest.models

enum class GiteaVisibilityEnum {
  PUBLIC("public"),
  LIMITED("limited"),
  PRIVATE("private");

  val value: String

  constructor(value: String) {
    this.value = value
  }
}