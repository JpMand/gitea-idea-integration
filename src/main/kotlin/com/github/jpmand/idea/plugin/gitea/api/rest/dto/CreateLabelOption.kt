package com.github.jpmand.idea.plugin.gitea.api.rest.dto


/**
 * CreateLabelOption options for creating a label
 * @param color
 * @param description Description provides additional context about the label's purpose
 * @param exclusive
 * @param isArchived
 * @param name Name is the display name for the new label
 */
data class CreateLabelOption(
    val color: String,
    /* Description provides additional context about the label's purpose */
    val description: String? = null,
    val exclusive: Boolean? = null,
    val isArchived: Boolean? = null,
    /* Name is the display name for the new label */
    val name: String,
)

