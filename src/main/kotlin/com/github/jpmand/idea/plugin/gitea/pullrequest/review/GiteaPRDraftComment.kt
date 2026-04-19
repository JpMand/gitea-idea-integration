package com.github.jpmand.idea.plugin.gitea.pullrequest.review

/**
 * A review comment that has been drafted locally but not yet submitted to the server.
 *
 * Positions mirror [GiteaReviewComment]: nullable means "not applicable for this diff side".
 * Converted to 0 at the DTO boundary when posting the review.
 */
data class GiteaPRDraftComment(
    /** Local-only identifier; never sent to the server. */
    val localId: Long,
    val path: String,
    /** 1-indexed line in the head (new) file; null for base-only (deleted-line) comments. */
    val newPosition: Int?,
    /** 1-indexed line in the base (old) file; null for head-only (added-line) comments. */
    val oldPosition: Int?,
    val body: String,
)
