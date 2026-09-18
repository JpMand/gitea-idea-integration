package com.github.jpmand.idea.plugin.gitea.api.rest.dto


/**
 * FileLinksResponse contains the links for a repo's file
 * @param git GitURL is the Git API URL for this file
 * @param html HTMLURL is the web URL for this file
 * @param self Self is the API URL for this file
 */
data class FileLinksResponse(
    /* GitURL is the Git API URL for this file */
    val git: String? = null,
    /* HTMLURL is the web URL for this file */
    val html: String? = null,
    /* Self is the API URL for this file */
    val self: String? = null,
)

