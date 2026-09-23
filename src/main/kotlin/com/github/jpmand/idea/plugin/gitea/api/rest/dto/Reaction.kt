package com.github.jpmand.idea.plugin.gitea.api.rest.dto

import java.time.OffsetDateTime

/**
 * Reaction contain one reaction
 * @param content The reaction content (e.g., emoji or reaction type)
 * @param createdAt The date and time when the reaction was created
 * @param user
 */
data class Reaction(
    /* The reaction content (e.g., emoji or reaction type) */
    val content: String? = null,
    /* The date and time when the reaction was created */
    val createdAt: OffsetDateTime? = null,
    val user: User? = null,
)

