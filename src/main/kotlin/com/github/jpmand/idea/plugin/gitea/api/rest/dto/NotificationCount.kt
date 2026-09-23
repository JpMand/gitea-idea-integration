package com.github.jpmand.idea.plugin.gitea.api.rest.dto


/**
 * NotificationCount number of unread notifications
 * @param new New is the number of unread notifications
 */
data class NotificationCount(
    /* New is the number of unread notifications */
    val new: Long? = null,
)

