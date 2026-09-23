package com.github.jpmand.idea.plugin.gitea.pullrequest.ui.action

import com.github.jpmand.idea.plugin.gitea.api.models.GiteaPullRequest
import com.github.jpmand.idea.plugin.gitea.pullrequest.ui.GiteaPRActionKeys
import com.github.jpmand.idea.plugin.gitea.util.GiteaBundle
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ide.CopyPasteManager
import java.awt.datatransfer.StringSelection

/**
 * Read-only / navigational context-menu actions for a PR list row. Built programmatically (not
 * registered in plugin.xml) so they can capture the list's callbacks; installed via
 * `PopupHandler.installPopupMenu` in `GiteaPRListPanel`.
 */
internal abstract class GiteaPRSelectionAction(text: String) : AnAction(text) {

    final override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    final override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = e.getData(GiteaPRActionKeys.SELECTED_PULL_REQUEST) != null
    }

    final override fun actionPerformed(e: AnActionEvent) {
        e.getData(GiteaPRActionKeys.SELECTED_PULL_REQUEST)?.let { perform(it) }
    }

    protected abstract fun perform(pr: GiteaPullRequest)
}

internal class GiteaPROpenInBrowserAction :
    GiteaPRSelectionAction(GiteaBundle.message("pull.request.action.open.pr.in.browser")) {
    override fun perform(pr: GiteaPullRequest) = BrowserUtil.browse(pr.htmlUrl)
}

internal class GiteaPRCopyLinkAction :
    GiteaPRSelectionAction(GiteaBundle.message("pull.request.action.copy.pr.link")) {
    override fun perform(pr: GiteaPullRequest) =
        CopyPasteManager.getInstance().setContents(StringSelection(pr.htmlUrl))
}

/** Not PR-scoped — always enabled; opens the repository's web page. */
internal class GiteaPROpenRepositoryAction(private val repositoryWebUrl: String) :
    AnAction(GiteaBundle.message("pull.request.action.open.repo.in.browser")) {
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = repositoryWebUrl.isNotBlank()
    }
    override fun actionPerformed(e: AnActionEvent) = BrowserUtil.browse(repositoryWebUrl)
}
