package com.github.jpmand.idea.plugin.gitea.api.rest.dto

/**
 * Tag represents a repository tag
 * @param commit
 * @param id The ID (SHA) of the tag
 * @param message The message associated with the tag
 * @param name The name of the tag
 * @param tarballUrl The URL to download the tarball archive
 * @param zipballUrl The URL to download the zipball archive
 */
data class Tag(
    val commit: CommitMeta? = null,
    /* The ID (SHA) of the tag */
    val id: String? = null,
    /* The message associated with the tag */
    val message: String? = null,
    /* The name of the tag */
    val name: String? = null,
    /* The URL to download the tarball archive */
    val tarballUrl: String? = null,
    /* The URL to download the zipball archive */
    val zipballUrl: String? = null,
)

