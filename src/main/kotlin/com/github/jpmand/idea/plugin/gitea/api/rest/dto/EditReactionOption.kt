package com.github.jpmand.idea.plugin.gitea.api.rest.dto


/**
 * EditReactionOption contain the reaction type
 * @param content The reaction content (e.g., emoji or reaction type)
 */
data class EditReactionOption(
    /* The reaction content (e.g., emoji or reaction type) */
    val content: String? = null,
)

