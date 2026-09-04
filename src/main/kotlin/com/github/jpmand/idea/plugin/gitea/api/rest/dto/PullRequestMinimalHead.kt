package com.github.jpmand.idea.plugin.gitea.api.rest.dto

/**
 * 
 * @param ref
 * @param repo
 * @param sha
 */
data class PullRequestMinimalHead(
    val ref: String? = null,
    val repo: PullRequestMinimalHeadRepo? = null,
    val sha: String? = null,
)

