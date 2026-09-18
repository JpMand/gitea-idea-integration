package com.github.jpmand.idea.plugin.gitea.api.rest.dto

import java.time.OffsetDateTime


/**
 * WatchInfo represents an API watch status of one repository
 * @param createdAt The timestamp when the watch status was created
 * @param ignored Whether notifications for the repository are ignored
 * @param reason The reason for the current watch status
 * @param repositoryUrl The URL of the repository being watched
 * @param subscribed Whether the repository is being watched for notifications
 * @param url The URL for managing the watch status
 */
data class WatchInfo(
    /* The timestamp when the watch status was created */
    val createdAt: OffsetDateTime? = null,
    /* Whether notifications for the repository are ignored */
    val ignored: Boolean? = null,
    /* The reason for the current watch status */
    val reason: Any? = null,
    /* The URL of the repository being watched */
    val repositoryUrl: String? = null,
    /* Whether the repository is being watched for notifications */
    val subscribed: Boolean? = null,
    /* The URL for managing the watch status */
    val url: String? = null,
)

