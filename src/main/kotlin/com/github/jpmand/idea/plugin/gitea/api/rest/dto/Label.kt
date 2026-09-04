package com.github.jpmand.idea.plugin.gitea.api.rest.dto


/**
 * Label a label to an issue or a pr
 * @param color
 * @param description Description provides additional context about the label's purpose
 * @param exclusive
 * @param id ID is the unique identifier for the label
 * @param isArchived
 * @param name Name is the display name of the label
 * @param url URL is the API endpoint for accessing this label
 */
data class Label(
    val color: String? = null,
    /* Description provides additional context about the label's purpose */
    val description: String? = null,
    val exclusive: Boolean? = null,
    /* ID is the unique identifier for the label */
    val id: Long? = null,
    val isArchived: Boolean? = null,
    /* Name is the display name of the label */
    val name: String? = null,
    /* URL is the API endpoint for accessing this label */
    val url: String? = null,
)

