package com.github.jpmand.idea.plugin.gitea.api.models

/**
 * A synthetic grouping of review comments at the same diff location.
 *
 * Gitea has no server-side thread IDs; threads are constructed client-side by grouping
 * [GiteaReviewComment]s that share the same [path] and line ([newLine]/[oldLine]).
 * [id] equals the first comment's ID and is not a server entity.
 */
data class GiteaReviewThread(
    /** Synthetic ID — equals the first comment's [GiteaReviewComment.id]. */
    val id: Long,
    val path: String?,
    /** 1-indexed line in the head file; null for base-only comments. */
    val newLine: Int?,
    /** 1-indexed line in the base file; null for head-only comments. */
    val oldLine: Int?,
    val isResolved: Boolean,
    val comments: List<GiteaReviewComment>
)
