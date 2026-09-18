package com.github.jpmand.idea.plugin.gitea.api.rest.dto


/**
 * 
 * @param id The unique identifier of the user
 * @param login The username of the user
 */
data class UserMeta(
    /* The unique identifier of the user */
    val id: Long? = null,
    /* The username of the user */
    val login: String? = null,
)

