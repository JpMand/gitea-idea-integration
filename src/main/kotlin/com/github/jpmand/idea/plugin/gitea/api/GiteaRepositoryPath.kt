package com.github.jpmand.idea.plugin.gitea.api

import com.intellij.openapi.util.NlsSafe

class GiteaRepositoryPath(val owner: String, val repository: String) {

  fun toString(showOwner: Boolean) = if (showOwner) "$owner/$repository" else repository

  @NlsSafe
  override fun toString(): String = "$owner/$repository"

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is GiteaRepositoryPath) return false

    if (owner != other.owner) return false
    if (repository != other.repository) return false

    return true
  }

  override fun hashCode(): Int {
    var result = owner.hashCode()
    result = 31 * result + repository.hashCode()
    return result
  }
}