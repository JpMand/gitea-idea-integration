package com.github.jpmand.idea.plugin.gitea.api.rest.dto

import java.time.OffsetDateTime

/**
 * Package represents a package
 * @param createdAt The date and time when the package was created
 * @param creator
 * @param htmlUrl The HTML URL to view the package
 * @param id The unique identifier of the package
 * @param name The name of the package
 * @param owner
 * @param repository
 * @param type The type of the package (e.g., npm, maven, docker)
 * @param version The version of the package
 */
data class Package(
    /* The date and time when the package was created */
    val createdAt: OffsetDateTime? = null,
    val creator: User? = null,
    /* The HTML URL to view the package */
    val htmlUrl: String? = null,
    /* The unique identifier of the package */
    val id: Long? = null,
    /* The name of the package */
    val name: String? = null,
    val owner: User? = null,
    val repository: Repository? = null,
    /* The type of the package (e.g., npm, maven, docker) */
    val type: String? = null,
    /* The version of the package */
    val version: String? = null,
)

