package com.github.jpmand.idea.plugin.gitea.api.rest.dto

/**
 * WikiCommitList commit/revision list
 * @param commits The list of wiki commits
 * @param count The total count of commits
 */
data class WikiCommitList(
    /* The list of wiki commits */
    val commits: Array<WikiCommit>? = null,
    /* The total count of commits */
    val count: Long? = null,
)

