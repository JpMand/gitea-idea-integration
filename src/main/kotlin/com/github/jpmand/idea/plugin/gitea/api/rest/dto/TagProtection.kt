package com.github.jpmand.idea.plugin.gitea.api.rest.dto

import java.time.OffsetDateTime


/**
 * TagProtection represents a tag protection
 * @param createdAt The date and time when the tag protection was created
 * @param id The unique identifier of the tag protection
 * @param namePattern The pattern to match tag names for protection
 * @param updatedAt The date and time when the tag protection was last updated
 * @param whitelistTeams List of team names allowed to create/delete protected tags
 * @param whitelistUsernames List of usernames allowed to create/delete protected tags
 */
data class TagProtection(
    /* The date and time when the tag protection was created */
    val createdAt: OffsetDateTime? = null,
    /* The unique identifier of the tag protection */
    val id: Long? = null,
    /* The pattern to match tag names for protection */
    val namePattern: String? = null,
    /* The date and time when the tag protection was last updated */
    val updatedAt: OffsetDateTime? = null,
    /* List of team names allowed to create/delete protected tags */
    val whitelistTeams: Array<String>? = null,
    /* List of usernames allowed to create/delete protected tags */
    val whitelistUsernames: Array<String>? = null,
)

