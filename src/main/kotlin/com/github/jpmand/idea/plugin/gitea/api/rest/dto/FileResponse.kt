package com.github.jpmand.idea.plugin.gitea.api.rest.dto

/**
 * FileResponse contains information about a repo's file
 * @param commit
 * @param content
 * @param verification
 */
data class FileResponse(
    val commit: FileCommitResponse? = null,
    val content: ContentsResponse? = null,
    val verification: PayloadCommitVerification? = null,
)

