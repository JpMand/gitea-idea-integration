package com.github.jpmand.idea.plugin.gitea.api.rest.dto


/**
 * CreateTagProtectionOption options for creating a tag protection
 * @param namePattern The pattern to match tag names for protection
 * @param whitelistTeams List of team names allowed to create/delete protected tags
 * @param whitelistUsernames List of usernames allowed to create/delete protected tags
 */
data class CreateTagProtectionOption(
    /* The pattern to match tag names for protection */
    val namePattern: String? = null,
    /* List of team names allowed to create/delete protected tags */
    val whitelistTeams: Array<String>? = null,
    /* List of usernames allowed to create/delete protected tags */
    val whitelistUsernames: Array<String>? = null,
)

