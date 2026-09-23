package com.github.jpmand.idea.plugin.gitea.api.rest.dto


/**
 * PackageFile represents a package file
 * @param id The unique identifier of the package file
 * @param md5 The MD5 hash of the package file
 * @param name The name of the package file
 * @param sha1 The SHA1 hash of the package file
 * @param sha256 The SHA256 hash of the package file
 * @param sha512 The SHA512 hash of the package file
 * @param size The size of the package file in bytes
 */
data class PackageFile(
    /* The unique identifier of the package file */
    val id: Long? = null,
    /* The MD5 hash of the package file */
    val md5: String? = null,
    /* The name of the package file */
    val name: String? = null,
    /* The SHA1 hash of the package file */
    val sha1: String? = null,
    /* The SHA256 hash of the package file */
    val sha256: String? = null,
    /* The SHA512 hash of the package file */
    val sha512: String? = null,
    /* The size of the package file in bytes */
    val size: Long? = null,
)

