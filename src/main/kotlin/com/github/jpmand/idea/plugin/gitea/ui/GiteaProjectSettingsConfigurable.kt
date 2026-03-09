package com.github.jpmand.idea.plugin.gitea.ui

import com.github.jpmand.idea.plugin.gitea.util.GiteaUtil.SERVICE_DISPLAY_NAME
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel

internal class GiteaProjectSettingsConfigurable internal constructor (private val project : Project) : BoundConfigurable(SERVICE_DISPLAY_NAME, "settings.gitea") {
  override fun createPanel(): DialogPanel {
    TODO("Not yet implemented")
  }
}