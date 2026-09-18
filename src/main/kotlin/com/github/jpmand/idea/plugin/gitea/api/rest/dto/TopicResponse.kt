package com.github.jpmand.idea.plugin.gitea.api.rest.dto

import java.time.OffsetDateTime


/**
 * TopicResponse for returning topics
 * @param created The date and time when the topic was created
 * @param id The unique identifier of the topic
 * @param repoCount The number of repositories using this topic
 * @param topicName The name of the topic
 * @param updated The date and time when the topic was last updated
 */
data class TopicResponse(
    /* The date and time when the topic was created */
    val created: OffsetDateTime? = null,
    /* The unique identifier of the topic */
    val id: Long? = null,
    /* The number of repositories using this topic */
    val repoCount: Long? = null,
    /* The name of the topic */
    val topicName: String? = null,
    /* The date and time when the topic was last updated */
    val updated: OffsetDateTime? = null,
)

