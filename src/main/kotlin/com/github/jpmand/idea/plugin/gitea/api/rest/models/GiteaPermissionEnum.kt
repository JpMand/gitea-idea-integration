package com.github.jpmand.idea.plugin.gitea.api.rest.models

enum class GiteaPermissionEnum {
  READ("read"),
  WRITE("write"),
  ADMIN("admin"),
  OWNER("owner");

  val value: String

  constructor(value: String) {
    this.value = value
  }
}