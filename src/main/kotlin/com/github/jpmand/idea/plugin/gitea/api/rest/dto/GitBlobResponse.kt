package com.github.jpmand.idea.plugin.gitea.api.rest.dto


/**
 * GitBlobResponse represents a git blob
 * @param content The content of the git blob (may be base64 encoded)
 * @param encoding The encoding used for the content (e.g., \"base64\")
 * @param lfsOid The LFS object ID if this blob is stored in LFS
 * @param lfsSize The size of the LFS object if this blob is stored in LFS
 * @param sha The SHA hash of the git blob
 * @param size The size of the git blob in bytes
 * @param url The URL to access this git blob
 */
data class GitBlobResponse(
    /* The content of the git blob (may be base64 encoded) */
    val content: String? = null,
    /* The encoding used for the content (e.g., \"base64\") */
    val encoding: String? = null,
    /* The LFS object ID if this blob is stored in LFS */
    val lfsOid: String? = null,
    /* The size of the LFS object if this blob is stored in LFS */
    val lfsSize: Long? = null,
    /* The SHA hash of the git blob */
    val sha: String? = null,
    /* The size of the git blob in bytes */
    val size: Long? = null,
    /* The URL to access this git blob */
    val url: String? = null,
)

