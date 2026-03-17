package com.github.jpmand.idea.plugin.gitea.api.rest.models.pr

enum class GiteaPullRequestSortEnum {
  OLDEST("oldest"),
  RECENTUPDATE("recentupdate"),
  RECENTCLOSE("recentclose"),
  LEASTUPDATE("leastupdate"),
  MOSTCOMMENT("mostcomment"),
  LEASTCOMMENT("leastcomment"),
  PRIORITY("priority");

  val value: String

  constructor(value: String) {
    this.value = value
  }
}