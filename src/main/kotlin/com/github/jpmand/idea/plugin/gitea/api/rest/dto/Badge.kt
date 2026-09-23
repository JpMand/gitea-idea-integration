package com.github.jpmand.idea.plugin.gitea.api.rest.dto


/**
 * Badge represents a user badge
 * @param description
 * @param id
 * @param imageUrl
 * @param slug
 */
data class Badge(
    val description: String? = null,
    val id: Long? = null,
    val imageUrl: String? = null,
    val slug: String? = null,
)

