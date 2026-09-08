package com.github.jpmand.idea.plugin.gitea.pullrequest.ui.action

import com.github.jpmand.idea.plugin.gitea.util.GiteaBundle
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages

/**
 * Milestone-2 write interactions (comment / review / merge / close / …) are not implemented yet.
 * The UI for them is scaffolded so the shape is visible, but every control routes here.
 */
internal fun giteaWriteActionNotImplemented(project: Project?, action: String) {
    Messages.showInfoMessage(
        project,
        GiteaBundle.message("pull.request.action.not.implemented.message", action),
        GiteaBundle.message("pull.request.action.not.implemented.title"),
    )
}
