package com.github.jpmand.idea.plugin.gitea.api.rest.dto


/**
 * RenameUserOption options when renaming a user
 * @param newUsername New username for this user. This name cannot be in use yet by any other user.
 */
data class RenameUserOption(
    /* New username for this user. This name cannot be in use yet by any other user. */
    val newUsername: String,
)

