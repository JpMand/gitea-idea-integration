package com.github.jpmand.idea.plugin.gitea.api.rest.dto


/**
 * CreateAccessTokenOption options when create access token
 * @param name
 * @param scopes
 */
data class CreateAccessTokenOption(
    val name: String,
    val scopes: Array<String>? = null,
)

