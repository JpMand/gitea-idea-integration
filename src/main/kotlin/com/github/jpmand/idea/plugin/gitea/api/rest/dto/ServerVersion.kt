package com.github.jpmand.idea.plugin.gitea.api.rest.dto


/**
 * ServerVersion wraps the version of the server
 * @param version Version is the server version string
 */
data class ServerVersion(
    /* Version is the server version string */
    val version: String? = null,
)

