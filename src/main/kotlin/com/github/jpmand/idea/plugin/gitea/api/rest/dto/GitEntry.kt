package com.github.jpmand.idea.plugin.gitea.api.rest.dto


/**
 * GitEntry represents a git tree
 * @param mode Mode is the file mode (permissions)
 * @param path Path is the file or directory path
 * @param sha SHA is the Git object SHA
 * @param size Size is the file size in bytes
 * @param type Type indicates if this is a file, directory, or symlink
 * @param url URL is the API URL for this tree entry
 */
data class GitEntry(
    /* Mode is the file mode (permissions) */
    val mode: String? = null,
    /* Path is the file or directory path */
    val path: String? = null,
    /* SHA is the Git object SHA */
    val sha: String? = null,
    /* Size is the file size in bytes */
    val size: Long? = null,
    /* Type indicates if this is a file, directory, or symlink */
    val type: String? = null,
    /* URL is the API URL for this tree entry */
    val url: String? = null,
)

