package com.github.jpmand.idea.plugin.gitea.api.rest.dto

import java.time.OffsetDateTime

/**
 * PublicKey publickey is a user key to push code to repository
 * @param createdAt Created is the time when the key was added
 * @param fingerprint Fingerprint is the key's fingerprint
 * @param id ID is the unique identifier for the public key
 * @param key Key contains the actual SSH public key content
 * @param keyType KeyType indicates the type of the SSH key
 * @param lastUsedAt Updated is the time when the key was last used
 * @param readOnly ReadOnly indicates if the key has read-only access
 * @param title Title is the human-readable name for the key
 * @param url URL is the API URL for this key
 * @param user
 */
data class PublicKey(
    /* Created is the time when the key was added */
    val createdAt: OffsetDateTime? = null,
    /* Fingerprint is the key's fingerprint */
    val fingerprint: String? = null,
    /* ID is the unique identifier for the public key */
    val id: Long? = null,
    /* Key contains the actual SSH public key content */
    val key: String? = null,
    /* KeyType indicates the type of the SSH key */
    val keyType: String? = null,
    /* Updated is the time when the key was last used */
    val lastUsedAt: OffsetDateTime? = null,
    /* ReadOnly indicates if the key has read-only access */
    val readOnly: Boolean? = null,
    /* Title is the human-readable name for the key */
    val title: String? = null,
    /* URL is the API URL for this key */
    val url: String? = null,
    val user: User? = null,
)

