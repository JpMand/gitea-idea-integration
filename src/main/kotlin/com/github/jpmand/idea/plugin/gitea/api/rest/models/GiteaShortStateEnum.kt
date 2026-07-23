package com.github.jpmand.idea.plugin.gitea.api.rest.models

enum class GiteaShortStateEnum {
  OPEN("open"),
  CLOSED("closed");

  val value: String

  constructor(value: String) {
    this.value = value
  }
}