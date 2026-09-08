package com.github.jpmand.idea.plugin.gitea.api.rest.dto


/**
 * UpdateVariableOption the option when updating variable
 * @param description Description of the variable to update
 * @param name New name for the variable. If the field is empty, the variable name won't be updated.
 * @param value Value of the variable to update
 */
data class UpdateVariableOption(
    /* Description of the variable to update */
    val description: String? = null,
    /* New name for the variable. If the field is empty, the variable name won't be updated. */
    val name: String? = null,
    /* Value of the variable to update */
    val value: String,
)

