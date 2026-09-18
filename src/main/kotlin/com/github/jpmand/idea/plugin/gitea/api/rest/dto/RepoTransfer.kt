package com.github.jpmand.idea.plugin.gitea.api.rest.dto

/**
 * RepoTransfer represents a pending repo transfer
 * @param doer
 * @param recipient
 * @param teams
 */
data class RepoTransfer(
    val doer: User? = null,
    val recipient: User? = null,
    val teams: Array<Team>? = null,
)

