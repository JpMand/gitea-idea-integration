package com.github.jpmand.idea.plugin.gitea.pullrequest.ui

import com.github.jpmand.idea.plugin.gitea.pullrequest.review.GiteaPRDiscussionsViewModels
import com.github.jpmand.idea.plugin.gitea.util.GiteaBundle
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
