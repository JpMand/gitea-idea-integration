package com.github.jpmand.idea.plugin.gitea.api.rest.dto


/**
 * IssueMeta basic issue information
 * @param index
 * @param owner owner of the issue's repo
 * @param repo
 */
data class IssueMeta(
    val index: Long? = null,
    /* owner of the issue's repo */
    val owner: String? = null,
    val repo: String? = null,
)

