package com.github.jpmand.idea.plugin.gitea.api.rest.dto


/**
 * CreateGPGKeyOption options create user GPG key
 * @param armoredPublicKey An armored GPG key to add
 * @param armoredSignature An optional armored signature for the GPG key
 */
data class CreateGPGKeyOption(
    /* An armored GPG key to add */
    val armoredPublicKey: String,
    /* An optional armored signature for the GPG key */
    val armoredSignature: String? = null,
)

