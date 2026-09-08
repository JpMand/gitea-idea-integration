package com.github.jpmand.idea.plugin.gitea.api.rest.dto

/**
 * FilesResponse contains information about multiple files from a repo
 * @param commit
 * @param files Files contains the list of file contents and metadata
 * @param verification
 */
data class FilesResponse(
    val commit: FileCommitResponse? = null,
    /* Files contains the list of file contents and metadata */
    val files: Array<ContentsResponse>? = null,
    val verification: PayloadCommitVerification? = null,
)

