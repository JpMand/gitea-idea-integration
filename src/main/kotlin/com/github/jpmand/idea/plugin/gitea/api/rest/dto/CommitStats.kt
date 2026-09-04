package com.github.jpmand.idea.plugin.gitea.api.rest.dto


/**
 * CommitStats is statistics for a RepoCommit
 * @param additions Additions is the number of lines added
 * @param deletions Deletions is the number of lines deleted
 * @param total Total is the total number of lines changed
 */
data class CommitStats(
    /* Additions is the number of lines added */
    val additions: Long? = null,
    /* Deletions is the number of lines deleted */
    val deletions: Long? = null,
    /* Total is the total number of lines changed */
    val total: Long? = null,
)

