package com.github.jpmand.idea.plugin.gitea.pullrequest.ui

import com.github.jpmand.idea.plugin.gitea.pullrequest.review.GiteaPRDiscussionsViewModels
import com.github.jpmand.idea.plugin.gitea.pullrequest.review.GiteaSuggestion
import com.github.jpmand.idea.plugin.gitea.util.GiteaBundle
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageDialogBuilder

/**
 * Confirms with the user, then cancels the review-in-progress via
 * [GiteaPRDiscussionsViewModels.cancelReview] — shared by the Details tab's review composer and
 * the diff editor's toolbar popup so both surfaces ask the same question.
 */
fun confirmAndCancelReview(project: Project, discussionsVm: GiteaPRDiscussionsViewModels) {
    val confirmed = MessageDialogBuilder.yesNo(
        GiteaBundle.message("pull.request.action.cancel.review.confirm.title"),
        GiteaBundle.message("pull.request.action.cancel.review.confirm.message"),
    )
        .asWarning()
        .yesText(GiteaBundle.message("pull.request.action.cancel.review"))
        .ask(project)
    if (confirmed) discussionsVm.cancelReview()
}

/**
 * Stub for the "Apply suggestion" action's real apply logic (locate the local file, verify the
 * target range, replace it) — not wired up yet. Rendering (detecting a suggestion and showing
 * this action) ships ahead of it so the encoding/detection can be verified in `runIde` first; see
 * the client-side suggested-change feature plan.
 */
fun applySuggestionNotImplementedYet(project: Project, suggestion: GiteaSuggestion) {
    NotificationGroupManager.getInstance()
        .getNotificationGroup("Gitea")
        .createNotification(
            GiteaBundle.message("pull.request.action.apply.suggestion"),
            GiteaBundle.message("pull.request.action.apply.suggestion.not.implemented"),
            NotificationType.INFORMATION,
        )
        .notify(project)
}
