package com.github.jpmand.idea.plugin.gitea.api.rest.dto


/**
 * LicensesInfo contains information about a License
 * @param body Body contains the full text of the license
 * @param implementation Implementation contains license implementation details
 * @param key Key is the unique identifier for the license template
 * @param name Name is the display name of the license
 * @param url URL is the reference URL for the license
 */
data class LicenseTemplateInfo(
    /* Body contains the full text of the license */
    val body: String? = null,
    /* Implementation contains license implementation details */
    val implementation: String? = null,
    /* Key is the unique identifier for the license template */
    val key: String? = null,
    /* Name is the display name of the license */
    val name: String? = null,
    /* URL is the reference URL for the license */
    val url: String? = null,
)

