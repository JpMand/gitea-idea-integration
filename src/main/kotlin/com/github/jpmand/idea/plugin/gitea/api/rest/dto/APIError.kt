package com.github.jpmand.idea.plugin.gitea.api.rest.dto


/**
 * APIError is an api error with a message
 * @param message Message contains the error description
 * @param url URL contains the documentation URL for this error
 */
data class APIError(
    /* Message contains the error description */
    val message: String? = null,
    /* URL contains the documentation URL for this error */
    val url: String? = null,
)

