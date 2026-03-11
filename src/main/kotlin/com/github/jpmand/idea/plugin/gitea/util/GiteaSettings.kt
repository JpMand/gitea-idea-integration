package com.github.jpmand.idea.plugin.gitea.util

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.*
import kotlinx.serialization.Serializable

@Service(Service.Level.APP)
@State(
  name = "GiteaSettings",
  storages = [Storage("gitea.xml")],
  reportStatistic = false,
  category = SettingsCategory.TOOLS
)
class GiteaSettings : SerializablePersistentStateComponent<GiteaSettings.State>(State()) {
  @Serializable
  data class State(
    val isAutomaticallyMarkAsViewed: Boolean = false
  )

  var isAutomaticallyMarkAsViewed: Boolean
    get() = state.isAutomaticallyMarkAsViewed
    set(value) {
      updateState { it.copy(isAutomaticallyMarkAsViewed = value) }
    }

  companion object {
    fun getInstance(): GiteaSettings =
      ApplicationManager.getApplication().service<GiteaSettings>()
  }
}