package com.github.jpmand.idea.plugin.gitea.api.rest.dto


/**
 * DismissPullReviewOptions are options to dismiss a pull request review
 * @param message
 * @param priors
 */
data class DismissPullReviewOptions(
    val message: String? = null,
    val priors: Boolean? = null,
)

