package com.github.jpmand.idea.plugin.gitea.api.rest.dto

import java.time.OffsetDateTime

/**
 * GPGKey a user GPG key to sign commit and tag in repository
 * @param canCertify Whether the key can be used for certification
 * @param canEncryptComms Whether the key can be used for encrypting communications
 * @param canEncryptStorage Whether the key can be used for encrypting storage
 * @param canSign Whether the key can be used for signing
 * @param createdAt The date and time when the GPG key was created
 * @param emails List of email addresses associated with this GPG key
 * @param expiresAt The date and time when the GPG key expires
 * @param id The unique identifier of the GPG key
 * @param keyId The key ID of the GPG key
 * @param primaryKeyId The primary key ID of the GPG key
 * @param publicKey The public key content in armored format
 * @param subkeys List of subkeys of this GPG key
 * @param verified Whether the GPG key has been verified
 */
data class GPGKey(
    /* Whether the key can be used for certification */
    val canCertify: Boolean? = null,
    /* Whether the key can be used for encrypting communications */
    val canEncryptComms: Boolean? = null,
    /* Whether the key can be used for encrypting storage */
    val canEncryptStorage: Boolean? = null,
    /* Whether the key can be used for signing */
    val canSign: Boolean? = null,
    /* The date and time when the GPG key was created */
    val createdAt: OffsetDateTime? = null,
    /* List of email addresses associated with this GPG key */
    val emails: Array<GPGKeyEmail>? = null,
    /* The date and time when the GPG key expires */
    val expiresAt: OffsetDateTime? = null,
    /* The unique identifier of the GPG key */
    val id: Long? = null,
    /* The key ID of the GPG key */
    val keyId: String? = null,
    /* The primary key ID of the GPG key */
    val primaryKeyId: String? = null,
    /* The public key content in armored format */
    val publicKey: String? = null,
    /* List of subkeys of this GPG key */
    val subkeys: Array<GPGKey>? = null,
    /* Whether the GPG key has been verified */
    val verified: Boolean? = null,
)

