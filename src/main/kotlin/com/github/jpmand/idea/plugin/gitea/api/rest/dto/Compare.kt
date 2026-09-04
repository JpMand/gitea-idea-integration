package com.github.jpmand.idea.plugin.gitea.api.rest.dto

/**
 * 
 * @param commits
 * @param totalCommits
 */
data class Compare(
    val commits: Array<Commit>? = null,
    val totalCommits: Long? = null,
)

