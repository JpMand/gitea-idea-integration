package com.github.jpmand.idea.plugin.gitea.api.rest.dto


/**
 * CreateVariableOption the option when creating variable
 * @param description Description of the variable to create
 * @param value Value of the variable to create
 */
data class CreateVariableOption(
    /* Description of the variable to create */
    val description: String? = null,
    /* Value of the variable to create */
    val value: String,
)

