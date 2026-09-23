package com.github.jpmand.idea.plugin.gitea.api.rest.dto

import java.time.OffsetDateTime


/**
 * 
 * @param createdAt The timestamp when the token was created
 * @param id The unique identifier of the access token
 * @param lastUsedAt The timestamp when the token was last used
 * @param name The name of the access token
 * @param scopes The scopes granted to this access token
 * @param sha1 The SHA1 hash of the access token
 * @param tokenLastEight The last eight characters of the token
 */
data class AccessToken(
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
    /* The SHA1 hash of the access token */
    val sha1: String? = null,
    /* The last eight characters of the token */
    val tokenLastEight: String? = null,
)

