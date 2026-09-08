package com.github.jpmand.idea.plugin.gitea.api.rest.dto


/**
 * Permission represents a set of permissions
 * @param admin
 * @param pull
 * @param push
 */
data class Permission(
    val admin: Boolean? = null,
    val pull: Boolean? = null,
    val push: Boolean? = null,
)

