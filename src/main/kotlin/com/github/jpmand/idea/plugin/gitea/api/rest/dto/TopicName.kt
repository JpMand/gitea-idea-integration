package com.github.jpmand.idea.plugin.gitea.api.rest.dto


/**
 * TopicName a list of repo topic names
 * @param topics List of topic names
 */
data class TopicName(
    /* List of topic names */
    val topics: Array<String>? = null,
)

