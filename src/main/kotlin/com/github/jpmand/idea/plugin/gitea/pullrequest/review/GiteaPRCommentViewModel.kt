package com.github.jpmand.idea.plugin.gitea.pullrequest.review

import com.github.jpmand.idea.plugin.gitea.api.models.GiteaReviewComment
import com.github.jpmand.idea.plugin.gitea.api.models.GiteaUser
import java.util.Date

/** ViewModel for a single review comment within a [GiteaPRThreadViewModel]. */
class GiteaPRCommentViewModel(val comment: GiteaReviewComment) {
    val id: Long get() = comment.id
    val author: GiteaUser? get() = comment.author
    val body: String? get() = comment.body
    val createdAt: Date? get() = comment.createdAt
    val updatedAt: Date? get() = comment.updatedAt
    val isResolved: Boolean get() = comment.isResolved
}
