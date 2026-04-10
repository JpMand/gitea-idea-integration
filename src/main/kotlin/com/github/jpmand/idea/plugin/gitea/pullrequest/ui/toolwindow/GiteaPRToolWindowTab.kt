package com.github.jpmand.idea.plugin.gitea.pullrequest.ui.toolwindow

import com.intellij.collaboration.ui.toolwindow.ReviewTab
import org.jetbrains.annotations.NonNls

sealed interface GiteaPRToolWindowTab: ReviewTab{
  data class PullRequest(val prId: String) : GiteaPRToolWindowTab {
    override val id: @NonNls String = "Review Details: $prId"
    override val reuseTabOnRequest: Boolean = true
  }

  data object NewPullRequest: GiteaPRToolWindowTab {
    override val id: @NonNls String = "New Pull Request"
    override val reuseTabOnRequest: Boolean = true
  }
}