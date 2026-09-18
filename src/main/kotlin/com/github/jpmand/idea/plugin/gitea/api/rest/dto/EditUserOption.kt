package com.github.jpmand.idea.plugin.gitea.api.rest.dto


/**
 * EditUserOption edit user options
 * @param active Whether the user account is active
 * @param admin Whether the user has administrator privileges
 * @param allowCreateOrganization Whether the user can create organizations
 * @param allowGitHook Whether the user can use Git hooks
 * @param allowImportLocal Whether the user can import local repositories
 * @param description The user's personal description or bio
 * @param email The email address of the user
 * @param fullName The full display name of the user
 * @param location The user's location or address
 * @param loginName identifier of the user, provided by the external authenticator (if configured)
 * @param maxRepoCreation Maximum number of repositories the user can create
 * @param mustChangePassword Whether the user must change password on next login
 * @param password The plain text password for the user
 * @param prohibitLogin Whether the user is prohibited from logging in
 * @param restricted Whether the user has restricted access privileges
 * @param sourceId The authentication source ID to associate with the user
 * @param visibility User visibility level: public, limited, or private
 * @param website The user's personal website URL
 */
data class EditUserOption(
    /* Whether the user account is active */
    val active: Boolean? = null,
    /* Whether the user has administrator privileges */
    val admin: Boolean? = null,
    /* Whether the user can create organizations */
    val allowCreateOrganization: Boolean? = null,
    /* Whether the user can use Git hooks */
    val allowGitHook: Boolean? = null,
    /* Whether the user can import local repositories */
    val allowImportLocal: Boolean? = null,
    /* The user's personal description or bio */
    val description: String? = null,
    /* The email address of the user */
    val email: String? = null,
    /* The full display name of the user */
    val fullName: String? = null,
    /* The user's location or address */
    val location: String? = null,
    /* identifier of the user, provided by the external authenticator (if configured) */
    val loginName: String,
    /* Maximum number of repositories the user can create */
    val maxRepoCreation: Long? = null,
    /* Whether the user must change password on next login */
    val mustChangePassword: Boolean? = null,
    /* The plain text password for the user */
    val password: String? = null,
    /* Whether the user is prohibited from logging in */
    val prohibitLogin: Boolean? = null,
    /* Whether the user has restricted access privileges */
    val restricted: Boolean? = null,
    /* The authentication source ID to associate with the user */
    val sourceId: Long,
    /* User visibility level: public, limited, or private */
    val visibility: Visibility? = null,
    /* The user's personal website URL */
    val website: String? = null,
) {


    /**
     * User visibility level: public, limited, or private
     * Values: PUBLIC,LIMITED,PRIVATE
     */
    enum class Visibility(val value: String) {

        PUBLIC("public"),

        LIMITED("limited"),

        PRIVATE("private");

    }


}

