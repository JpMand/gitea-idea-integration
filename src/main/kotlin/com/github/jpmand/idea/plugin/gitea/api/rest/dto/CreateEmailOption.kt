package com.github.jpmand.idea.plugin.gitea.api.rest.dto


/**
 * CreateEmailOption options when creating email addresses
 * @param emails email addresses to add
 */
data class CreateEmailOption(
    /* email addresses to add */
    val emails: Array<String>? = null,
)

