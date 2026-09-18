package com.github.jpmand.idea.plugin.gitea.api.rest.dto

import java.time.OffsetDateTime

/**
 * DeployKey a deploy key
 * @param createdAt Created is the time when the deploy key was added
 * @param fingerprint Fingerprint is the key's fingerprint
 * @param id ID is the unique identifier for the deploy key
 * @param key Key contains the actual SSH key content
 * @param keyId KeyID is the associated public key ID
 * @param readOnly ReadOnly indicates if the key has read-only access
 * @param repository
 * @param title Title is the human-readable name for the key
 * @param url URL is the API URL for this deploy key
 */
data class DeployKey(
    /* Created is the time when the deploy key was added */
    val createdAt: OffsetDateTime? = null,
    /* Fingerprint is the key's fingerprint */
    val fingerprint: String? = null,
    /* ID is the unique identifier for the deploy key */
    val id: Long? = null,
    /* Key contains the actual SSH key content */
    val key: String? = null,
    /* KeyID is the associated public key ID */
    val keyId: Long? = null,
    /* ReadOnly indicates if the key has read-only access */
    val readOnly: Boolean? = null,
    val repository: Repository? = null,
    /* Title is the human-readable name for the key */
    val title: String? = null,
    /* URL is the API URL for this deploy key */
    val url: String? = null,
)

