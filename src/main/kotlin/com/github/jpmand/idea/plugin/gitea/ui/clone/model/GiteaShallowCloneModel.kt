package com.github.jpmand.idea.plugin.gitea.ui.clone.model

import git4idea.commands.GitShallowCloneOptions
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Shallow-clone toggle + depth for the clone dialog. Replaces git4idea's internal
 * `GitShallowCloneViewModel`; produces the public [GitShallowCloneOptions].
 */
internal class GiteaShallowCloneModel {
  val shallowClone = MutableStateFlow(false)
  val depth = MutableStateFlow(1)

  fun toOptions(): GitShallowCloneOptions? =
    if (shallowClone.value) GitShallowCloneOptions(depth.value.coerceAtLeast(1)) else null
}
