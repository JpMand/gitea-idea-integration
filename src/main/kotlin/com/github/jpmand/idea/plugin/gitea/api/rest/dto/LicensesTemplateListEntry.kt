package com.github.jpmand.idea.plugin.gitea.api.rest.dto


/**
 * LicensesListEntry is used for the API
 * @param key Key is the unique identifier for the license template
 * @param name Name is the display name of the license
 * @param url URL is the reference URL for the license
 */
data class LicensesTemplateListEntry(
    /* Key is the unique identifier for the license template */
    val key: String? = null,
    /* Name is the display name of the license */
    val name: String? = null,
    /* URL is the reference URL for the license */
    val url: String? = null,
)

