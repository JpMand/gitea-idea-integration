package com.github.jpmand.idea.plugin.gitea.api.rest.dto


/**
 * GetFilesOptions options for retrieving metadate and content of multiple files
 * @param files Files is the list of file paths to retrieve
 */
data class GetFilesOptions(
    /* Files is the list of file paths to retrieve */
    val files: Array<String>? = null,
)

