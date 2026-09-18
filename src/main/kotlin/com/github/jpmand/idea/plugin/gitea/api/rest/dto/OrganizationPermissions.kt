package com.github.jpmand.idea.plugin.gitea.api.rest.dto


/**
 * OrganizationPermissions list different users permissions on an organization
 * @param canCreateRepository Whether the user can create repositories in the organization
 * @param canRead Whether the user can read the organization
 * @param canWrite Whether the user can write to the organization
 * @param isAdmin Whether the user is an admin of the organization
 * @param isOwner Whether the user is an owner of the organization
 */
data class OrganizationPermissions(
    /* Whether the user can create repositories in the organization */
    val canCreateRepository: Boolean? = null,
    /* Whether the user can read the organization */
    val canRead: Boolean? = null,
    /* Whether the user can write to the organization */
    val canWrite: Boolean? = null,
    /* Whether the user is an admin of the organization */
    val isAdmin: Boolean? = null,
    /* Whether the user is an owner of the organization */
    val isOwner: Boolean? = null,
)

