package com.github.jpmand.idea.plugin.gitea.api.rest.dto


/**
 * RepositoryMeta basic repository information
 * @param fullName
 * @param id
 * @param name
 * @param owner
 */
data class RepositoryMeta(
    val fullName: String? = null,
    val id: Long? = null,
    val name: String? = null,
    val owner: String? = null,
)

