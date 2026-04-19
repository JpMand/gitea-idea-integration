package com.github.jpmand.idea.plugin.gitea.api.models

import java.util.Date

data class GiteaReview(
    val id: Long,
    val author: GiteaUser?,
    val body: String?,
    val state: GiteaReviewState,
    val submittedAt: Date?,
    val dismissed: Boolean,
    val stale: Boolean,
    val commitId: String?
)
