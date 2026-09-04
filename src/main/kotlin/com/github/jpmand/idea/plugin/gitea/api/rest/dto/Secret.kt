package com.github.jpmand.idea.plugin.gitea.api.rest.dto

import java.time.OffsetDateTime


/**
 * Secret represents a secret
 * @param createdAt
 * @param description the secret's description
 * @param name the secret's name
 */
data class Secret(
    val createdAt: OffsetDateTime? = null,
    /* the secret's description */
    val description: String? = null,
    /* the secret's name */
    val name: String? = null,
)

