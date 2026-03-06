package com.github.jpmand.idea.plugin.gitea.util

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent

class GiteaSettings : PersistentStateComponent<GiteaSettings.State> {
  var myState = State()

  override fun getState(): State? = myState

  override fun loadState(state: State) {
    myState = state
  }
  companion object{
    fun getInstance(): GiteaSettings = ApplicationManager.getApplication().getService<GiteaSettings>(GiteaSettings::class.java)
  }
  inner class State {

  }
}