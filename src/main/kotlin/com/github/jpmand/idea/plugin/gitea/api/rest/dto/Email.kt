package com.github.jpmand.idea.plugin.gitea.api.rest.dto


/**
 * Email an email address belonging to a user
 * @param email The email address
 * @param primary Whether this is the primary email address
 * @param userId The unique identifier of the user who owns this email
 * @param username username of the user
 * @param verified Whether the email address has been verified
 */
data class Email(
    /* The email address */
    val email: String? = null,
    /* Whether this is the primary email address */
    val primary: Boolean? = null,
    /* The unique identifier of the user who owns this email */
    val userId: Long? = null,
    /* username of the user */
    val username: String? = null,
    /* Whether the email address has been verified */
    val verified: Boolean? = null,
)

