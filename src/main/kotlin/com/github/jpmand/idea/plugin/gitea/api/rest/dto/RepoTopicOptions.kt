package com.github.jpmand.idea.plugin.gitea.api.rest.dto


/**
 * RepoTopicOptions a collection of repo topic names
 * @param topics list of topic names
 */
data class RepoTopicOptions(
    /* list of topic names */
    val topics: Array<String>? = null,
)

