package com.github.jpmand.idea.plugin.gitea.api.rest.dto


/**
 * TransferRepoOption options when transfer a repository's ownership
 * @param newOwner
 * @param teamIds ID of the team or teams to add to the repository. Teams can only be added to organization-owned repositories.
 */
data class TransferRepoOption(
    val newOwner: String,
    /* ID of the team or teams to add to the repository. Teams can only be added to organization-owned repositories. */
    val teamIds: Array<Long>? = null,
)

