package com.github.jpmand.idea.plugin.gitea.api.rest.dto

import java.time.OffsetDateTime


/**
 * CreateUserOption create user options
 * @param createdAt For explicitly setting the user creation timestamp. Useful when users are migrated from other systems. When omitted, the user's creation timestamp will be set to \"now\".
 * @param email
 * @param fullName The full display name of the user
 * @param loginName identifier of the user, provided by the external authenticator (if configured)
 * @param mustChangePassword Whether the user must change password on first login
 * @param password The plain text password for the user
 * @param restricted Whether the user has restricted access privileges
 * @param sendNotify Whether to send welcome notification email to the user
 * @param sourceId The authentication source ID to associate with the user
 * @param username username of the user
 * @param visibility User visibility level: public, limited, or private
 */
data class CreateUserOption(
    /* For explicitly setting the user creation timestamp. Useful when users are migrated from other systems. When omitted, the user's creation timestamp will be set to \"now\". */
    val createdAt: OffsetDateTime? = null,
    val email: String,
    /* The full display name of the user */
    val fullName: String? = null,
    /* identifier of the user, provided by the external authenticator (if configured) */
    val loginName: String? = null,
    /* Whether the user must change password on first login */
    val mustChangePassword: Boolean? = null,
    /* The plain text password for the user */
    val password: String? = null,
    /* Whether the user has restricted access privileges */
    val restricted: Boolean? = null,
    /* Whether to send welcome notification email to the user */
    val sendNotify: Boolean? = null,
    /* The authentication source ID to associate with the user */
    val sourceId: Long? = null,
    /* username of the user */
    val username: String,
    /* User visibility level: public, limited, or private */
    val visibility: Visibility? = null,
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

