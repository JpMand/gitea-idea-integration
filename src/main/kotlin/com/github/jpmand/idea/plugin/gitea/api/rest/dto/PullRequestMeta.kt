package com.github.jpmand.idea.plugin.gitea.api.rest.dto

import java.time.OffsetDateTime


/**
 * PullRequestMeta PR info if an issue is a PR
 * @param draft
 * @param htmlUrl
 * @param merged
 * @param mergedAt
 */
data class PullRequestMeta(
    val draft: Boolean? = null,
    val htmlUrl: String? = null,
    val merged: Boolean? = null,
    val mergedAt: OffsetDateTime? = null,
)

