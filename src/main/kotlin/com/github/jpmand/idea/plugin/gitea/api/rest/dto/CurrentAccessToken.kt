package com.github.jpmand.idea.plugin.gitea.api.rest.dto

import java.time.OffsetDateTime

/**
 * 
 * @param createdAt The timestamp when the token was created
 * @param id The unique identifier of the access token
 * @param lastUsedAt The timestamp when the token was last used
 * @param name The name of the access token
 * @param scopes The scopes granted to this access token
 * @param user
 */
data class CurrentAccessToken(
    /* The timestamp when the token was created */
    val createdAt: OffsetDateTime? = null,
    /* The unique identifier of the access token */
    val id: Long? = null,
    /* The timestamp when the token was last used */
    val lastUsedAt: OffsetDateTime? = null,
    /* The name of the access token */
    val name: String? = null,
    /* The scopes granted to this access token */
    val scopes: Array<String>? = null,
    val user: UserMeta? = null,
)

