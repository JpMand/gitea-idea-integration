package com.github.jpmand.idea.plugin.gitea.api.rest.dto

/**
 * 
 * @param dirContents DirContents contains directory listing when the path represents a directory
 * @param fileContents
 */
data class ContentsExtResponse(
    /* DirContents contains directory listing when the path represents a directory */
    val dirContents: Array<ContentsResponse>? = null,
    val fileContents: ContentsResponse? = null,
)

