package com.github.jpmand.idea.plugin.gitea.api.rest.dto


/**
 * CreateOrgOption options for creating an organization
 * @param description The description of the organization
 * @param email The email address of the organization
 * @param fullName The full display name of the organization
 * @param location The location of the organization
 * @param repoAdminChangeTeamAccess Whether repository administrators can change team access
 * @param username username of the organization
 * @param visibility possible values are `public` (default), `limited` or `private`
 * @param website The website URL of the organization
 */
data class CreateOrgOption(
    /* The description of the organization */
    val description: String? = null,
    /* The email address of the organization */
    val email: String? = null,
    /* The full display name of the organization */
    val fullName: String? = null,
    /* The location of the organization */
    val location: String? = null,
    /* Whether repository administrators can change team access */
    val repoAdminChangeTeamAccess: Boolean? = null,
    /* username of the organization */
    val username: String,
    /* possible values are `public` (default), `limited` or `private` */
    val visibility: Visibility? = null,
    /* The website URL of the organization */
    val website: String? = null,
) {


    /**
     * possible values are `public` (default), `limited` or `private`
     * Values: PUBLIC,LIMITED,PRIVATE
     */
    enum class Visibility(val value: String) {

        PUBLIC("public"),

        LIMITED("limited"),

        PRIVATE("private");

    }


}

