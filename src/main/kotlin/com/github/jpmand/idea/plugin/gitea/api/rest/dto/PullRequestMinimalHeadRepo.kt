package com.github.jpmand.idea.plugin.gitea.api.rest.dto


/**
 * 
 * @param id
 * @param name
 * @param url
 */
data class PullRequestMinimalHeadRepo(
    val id: Long? = null,
    val name: String? = null,
    val url: String? = null,
)

