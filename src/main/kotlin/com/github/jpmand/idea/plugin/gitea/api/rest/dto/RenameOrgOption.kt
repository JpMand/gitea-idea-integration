package com.github.jpmand.idea.plugin.gitea.api.rest.dto


/**
 * RenameOrgOption options when renaming an organization
 * @param newName New username for this org. This name cannot be in use yet by any other user.
 */
data class RenameOrgOption(
    /* New username for this org. This name cannot be in use yet by any other user. */
    val newName: String,
)

