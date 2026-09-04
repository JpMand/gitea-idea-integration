package com.github.jpmand.idea.plugin.gitea.api.rest.dto


/**
 * ChangedFile store information about files affected by the pull request
 * @param additions The number of lines added to the file
 * @param changes The total number of changes to the file
 * @param contentsUrl The API URL to get the file contents
 * @param deletions The number of lines deleted from the file
 * @param filename The name of the changed file
 * @param htmlUrl The HTML URL to view the file changes
 * @param previousFilename The previous filename if the file was renamed
 * @param rawUrl The raw URL to download the file
 * @param status The status of the file change (added, modified, deleted, etc.)
 */
data class ChangedFile(
    /* The number of lines added to the file */
    val additions: Long? = null,
    /* The total number of changes to the file */
    val changes: Long? = null,
    /* The API URL to get the file contents */
    val contentsUrl: String? = null,
    /* The number of lines deleted from the file */
    val deletions: Long? = null,
    /* The name of the changed file */
    val filename: String? = null,
    /* The HTML URL to view the file changes */
    val htmlUrl: String? = null,
    /* The previous filename if the file was renamed */
    val previousFilename: String? = null,
    /* The raw URL to download the file */
    val rawUrl: String? = null,
    /* The status of the file change (added, modified, deleted, etc.) */
    val status: String? = null,
)

