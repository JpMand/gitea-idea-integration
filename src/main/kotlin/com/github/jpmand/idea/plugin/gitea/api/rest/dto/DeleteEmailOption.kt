package com.github.jpmand.idea.plugin.gitea.api.rest.dto


/**
 * DeleteEmailOption options when deleting email addresses
 * @param emails email addresses to delete
 */
data class DeleteEmailOption(
    /* email addresses to delete */
    val emails: Array<String>? = null,
)

