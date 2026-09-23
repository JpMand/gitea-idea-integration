package com.github.jpmand.idea.plugin.gitea.api.rest.dto

/**
 * FileDeleteResponse contains information about a repo's file that was deleted
 * @param commit
 * @param content Content is always null for delete operations
 * @param verification
 */
data class FileDeleteResponse(
    val commit: FileCommitResponse? = null,
    /* Content is always null for delete operations */
    val content: Any? = null,
    val verification: PayloadCommitVerification? = null,
)

