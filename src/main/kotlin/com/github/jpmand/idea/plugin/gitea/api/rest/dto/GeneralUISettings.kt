package com.github.jpmand.idea.plugin.gitea.api.rest.dto


/**
 * GeneralUISettings contains global ui settings exposed by API
 * @param allowedReactions AllowedReactions contains the list of allowed emoji reactions
 * @param customEmojis CustomEmojis contains the list of custom emojis
 * @param defaultTheme DefaultTheme is the default UI theme
 */
data class GeneralUISettings(
    /* AllowedReactions contains the list of allowed emoji reactions */
    val allowedReactions: Array<String>? = null,
    /* CustomEmojis contains the list of custom emojis */
    val customEmojis: Array<String>? = null,
    /* DefaultTheme is the default UI theme */
    val defaultTheme: String? = null,
)

