package com.github.jpmand.idea.plugin.gitea.api.rest.dto


/**
 * CreateForkOption options for creating a fork
 * @param name name of the forked repository
 * @param organization organization name, if forking into an organization
 */
data class CreateForkOption(
    /* name of the forked repository */
    val name: String? = null,
    /* organization name, if forking into an organization */
    val organization: String? = null,
)

