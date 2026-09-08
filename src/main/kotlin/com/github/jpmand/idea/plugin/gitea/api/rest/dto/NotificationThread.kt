package com.github.jpmand.idea.plugin.gitea.api.rest.dto

import java.time.OffsetDateTime

/**
 * NotificationThread expose Notification on API
 * @param id ID is the unique identifier for the notification thread
 * @param pinned Pinned indicates if the notification is pinned
 * @param repository
 * @param subject
 * @param unread Unread indicates if the notification has been read
 * @param updatedAt UpdatedAt is the time when the notification was last updated
 * @param url URL is the API URL for this notification thread
 */
data class NotificationThread(
    /* ID is the unique identifier for the notification thread */
    val id: Long? = null,
    /* Pinned indicates if the notification is pinned */
    val pinned: Boolean? = null,
    val repository: Repository? = null,
    val subject: NotificationSubject? = null,
    /* Unread indicates if the notification has been read */
    val unread: Boolean? = null,
    /* UpdatedAt is the time when the notification was last updated */
    val updatedAt: OffsetDateTime? = null,
    /* URL is the API URL for this notification thread */
    val url: String? = null,
)

