package com.github.jpmand.idea.plugin.gitea.api.rest.dto

import java.time.OffsetDateTime


/**
 * ActionWorkflow represents a ActionWorkflow
 * @param badgeUrl BadgeURL is the URL for the workflow badge
 * @param createdAt
 * @param deletedAt
 * @param htmlUrl HTMLURL is the web URL for viewing the workflow
 * @param id ID is the unique identifier for the workflow
 * @param name Name is the name of the workflow
 * @param path Path is the file path of the workflow
 * @param state State indicates if the workflow is active or disabled
 * @param updatedAt
 * @param url URL is the API URL for this workflow
 */
data class ActionWorkflow(
    /* BadgeURL is the URL for the workflow badge */
    val badgeUrl: String? = null,
    val createdAt: OffsetDateTime? = null,
    val deletedAt: OffsetDateTime? = null,
    /* HTMLURL is the web URL for viewing the workflow */
    val htmlUrl: String? = null,
    /* ID is the unique identifier for the workflow */
    val id: String? = null,
    /* Name is the name of the workflow */
    val name: String? = null,
    /* Path is the file path of the workflow */
    val path: String? = null,
    /* State indicates if the workflow is active or disabled */
    val state: String? = null,
    val updatedAt: OffsetDateTime? = null,
    /* URL is the API URL for this workflow */
    val url: String? = null,
)

