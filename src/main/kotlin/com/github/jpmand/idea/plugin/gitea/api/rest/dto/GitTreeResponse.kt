package com.github.jpmand.idea.plugin.gitea.api.rest.dto

/**
 * GitTreeResponse returns a git tree
 * @param page Page is the current page number for pagination
 * @param sha SHA is the tree object SHA
 * @param totalCount TotalCount is the total number of entries in the tree
 * @param tree Entries contains the tree entries (files and directories)
 * @param truncated Truncated indicates if the response was truncated due to size
 * @param url URL is the API URL for this tree
 */
data class GitTreeResponse(
    /* Page is the current page number for pagination */
    val page: Long? = null,
    /* SHA is the tree object SHA */
    val sha: String? = null,
    /* TotalCount is the total number of entries in the tree */
    val totalCount: Long? = null,
    /* Entries contains the tree entries (files and directories) */
    val tree: Array<GitEntry>? = null,
    /* Truncated indicates if the response was truncated due to size */
    val truncated: Boolean? = null,
    /* URL is the API URL for this tree */
    val url: String? = null,
)

