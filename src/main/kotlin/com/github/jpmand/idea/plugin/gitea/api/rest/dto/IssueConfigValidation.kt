package com.github.jpmand.idea.plugin.gitea.api.rest.dto


/**
 * 
 * @param message
 * @param valid
 */
data class IssueConfigValidation(
    val message: String? = null,
    val valid: Boolean? = null,
)

