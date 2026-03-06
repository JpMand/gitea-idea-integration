package com.github.jpmand.idea.plugin.gitea.ui

import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel

internal class GiteaProjectSettingsConfigurable internal constructor (private val project : Project) : BoundConfigurable("Gitea", "settings.gitea") {
  override fun createPanel(): DialogPanel {
    TODO("Not yet implemented")
  }
}