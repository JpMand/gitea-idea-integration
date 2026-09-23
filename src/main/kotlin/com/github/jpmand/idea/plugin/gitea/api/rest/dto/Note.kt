package com.github.jpmand.idea.plugin.gitea.api.rest.dto

/**
 * Note contains information related to a git note
 * @param commit
 * @param message The content message of the git note
 */
data class Note(
    val commit: Commit? = null,
    /* The content message of the git note */
    val message: String? = null,
)

