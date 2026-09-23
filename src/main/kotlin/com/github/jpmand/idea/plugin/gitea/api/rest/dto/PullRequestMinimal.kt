package com.github.jpmand.idea.plugin.gitea.api.rest.dto

/**
 * PullRequestMinimal is the minimal information about a pull request, as returned in the `pull_requests` field of a workflow run.
 * @param base
 * @param head
 * @param id
 * @param number
 * @param url
 */
data class PullRequestMinimal(
    val base: PullRequestMinimalHead? = null,
    val head: PullRequestMinimalHead? = null,
    val id: Long? = null,
    val number: Long? = null,
    val url: String? = null,
)

