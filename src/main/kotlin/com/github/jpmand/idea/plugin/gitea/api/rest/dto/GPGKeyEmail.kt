package com.github.jpmand.idea.plugin.gitea.api.rest.dto


/**
 * GPGKeyEmail an email attached to a GPGKey
 * @param email The email address associated with the GPG key
 * @param verified Whether the email address has been verified
 */
data class GPGKeyEmail(
    /* The email address associated with the GPG key */
    val email: String? = null,
    /* Whether the email address has been verified */
    val verified: Boolean? = null,
)

