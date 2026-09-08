package com.github.jpmand.idea.plugin.gitea.api.rest.dto


/**
 * LabelTemplate info of a Label template
 * @param color
 * @param description Description provides additional context about the label template's purpose
 * @param exclusive
 * @param name Name is the display name of the label template
 */
data class LabelTemplate(
    val color: String? = null,
    /* Description provides additional context about the label template's purpose */
    val description: String? = null,
    val exclusive: Boolean? = null,
    /* Name is the display name of the label template */
    val name: String? = null,
)

