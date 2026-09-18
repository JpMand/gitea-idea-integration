package com.github.jpmand.idea.plugin.gitea.api.rest.dto


/**
 * Organization represents an organization
 * @param avatarUrl The URL of the organization's avatar
 * @param description The description of the organization
 * @param email The email address of the organization
 * @param fullName The full display name of the organization
 * @param id The unique identifier of the organization
 * @param location The location of the organization
 * @param name The name of the organization
 * @param repoAdminChangeTeamAccess Whether repository administrators can change team access
 * @param username username of the organization deprecated
 * @param visibility The visibility level of the organization (public, limited, private)
 * @param website The website URL of the organization
 */
data class Organization(
    /* The URL of the organization's avatar */
    val avatarUrl: String? = null,
    /* The description of the organization */
    val description: String? = null,
    /* The email address of the organization */
    val email: String? = null,
    /* The full display name of the organization */
    val fullName: String? = null,
    /* The unique identifier of the organization */
    val id: Long? = null,
    /* The location of the organization */
    val location: String? = null,
    /* The name of the organization */
    val name: String? = null,
    /* Whether repository administrators can change team access */
    val repoAdminChangeTeamAccess: Boolean? = null,
    /* username of the organization deprecated */
    val username: String? = null,
    /* The visibility level of the organization (public, limited, private) */
    val visibility: Visibility? = null,
    /* The website URL of the organization */
    val website: String? = null,
) {


    /**
     * The visibility level of the organization (public, limited, private)
     * Values: PUBLIC,LIMITED,PRIVATE
     */
    enum class Visibility(val value: String) {

        PUBLIC("public"),

        LIMITED("limited"),

        PRIVATE("private");

    }


}

