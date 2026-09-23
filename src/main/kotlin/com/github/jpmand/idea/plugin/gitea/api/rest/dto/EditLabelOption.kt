package com.github.jpmand.idea.plugin.gitea.api.rest.dto


/**
 * EditLabelOption options for editing a label
 * @param color
 * @param description Description provides additional context about the label's purpose
 * @param exclusive
 * @param isArchived
 * @param name Name is the new display name for the label
 */
data class EditLabelOption(
    val color: String? = null,
    /* Description provides additional context about the label's purpose */
    val description: String? = null,
    val exclusive: Boolean? = null,
    val isArchived: Boolean? = null,
    /* Name is the new display name for the label */
    val name: String? = null,
)

