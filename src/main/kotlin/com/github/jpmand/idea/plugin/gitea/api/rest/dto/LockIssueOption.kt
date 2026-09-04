package com.github.jpmand.idea.plugin.gitea.api.rest.dto


/**
 * LockIssueOption options to lock an issue
 * @param lockReason
 */
data class LockIssueOption(
    val lockReason: String? = null,
)

