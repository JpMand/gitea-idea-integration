package com.github.jpmand.idea.plugin.gitea.api.rest.dto


/**
 * CreateWikiPageOptions form for creating wiki
 * @param contentBase64 content must be base64 encoded
 * @param message optional commit message summarizing the change
 * @param title page title. leave empty to keep unchanged
 */
data class CreateWikiPageOptions(
    /* content must be base64 encoded */
    val contentBase64: String? = null,
    /* optional commit message summarizing the change */
    val message: String? = null,
    /* page title. leave empty to keep unchanged */
    val title: String? = null,
)

