package com.github.jpmand.idea.plugin.gitea.pullrequest.ui.action

import com.github.jpmand.idea.plugin.gitea.api.models.GiteaPullRequest
import com.github.jpmand.idea.plugin.gitea.pullrequest.ui.GiteaPRActionKeys
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

/**
 * Opens the currently-selected PR (see [GiteaPRActionKeys.SELECTED_PULL_REQUEST]).
 *
 * Bound directly to the PR list component (via `registerCustomShortcutSet`, see
 * `GiteaPRListPanel`) rather than registered globally in plugin.xml, to avoid claiming the
 * Enter/double-click shortcuts application-wide.
 */
class GiteaPROpenPullRequestAction(
    private val onOpen: (GiteaPullRequest) -> Unit,
) : AnAction() {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = e.getData(GiteaPRActionKeys.SELECTED_PULL_REQUEST) != null
    }

    override fun actionPerformed(e: AnActionEvent) {
        val pr = e.getData(GiteaPRActionKeys.SELECTED_PULL_REQUEST) ?: return
        onOpen(pr)
    }
}
