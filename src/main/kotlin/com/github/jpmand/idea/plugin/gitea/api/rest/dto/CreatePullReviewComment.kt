package com.github.jpmand.idea.plugin.gitea.api.rest.dto


/**
 * CreatePullReviewComment represent a review comment for creation api
 * @param body
 * @param newPosition if comment to new file line or 0
 * @param oldPosition if comment to old file line or 0
 * @param path the tree path
 */
data class CreatePullReviewComment(
    val body: String? = null,
    /* if comment to new file line or 0 */
    val newPosition: Long? = null,
    /* if comment to old file line or 0 */
    val oldPosition: Long? = null,
    /* the tree path */
    val path: String? = null,
)

