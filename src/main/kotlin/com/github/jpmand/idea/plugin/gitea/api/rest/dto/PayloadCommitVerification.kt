package com.github.jpmand.idea.plugin.gitea.api.rest.dto

/**
 * PayloadCommitVerification represents the GPG verification of a commit
 * @param payload The signed payload content
 * @param reason The reason for the verification status
 * @param signature The GPG signature of the commit
 * @param signer
 * @param verified Whether the commit signature is verified
 */
data class PayloadCommitVerification(
    /* The signed payload content */
    val payload: String? = null,
    /* The reason for the verification status */
    val reason: String? = null,
    /* The GPG signature of the commit */
    val signature: String? = null,
    val signer: PayloadUser? = null,
    /* Whether the commit signature is verified */
    val verified: Boolean? = null,
)

