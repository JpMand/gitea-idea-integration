package com.github.jpmand.idea.plugin.gitea.api.rest.dto

import java.time.OffsetDateTime


/**
 * Hook a hook is a web hook when one repository changed
 * @param active Whether the webhook is active and will be triggered
 * @param authorizationHeader Authorization header to include in webhook requests
 * @param branchFilter Branch filter pattern to determine which branches trigger the webhook
 * @param config Configuration settings for the webhook
 * @param createdAt The date and time when the webhook was created
 * @param events List of events that trigger this webhook
 * @param id The unique identifier of the webhook
 * @param name Optional human-readable name for the webhook
 * @param type The type of the webhook (e.g., gitea, slack, discord)
 * @param updatedAt The date and time when the webhook was last updated
 */
data class Hook(
    /* Whether the webhook is active and will be triggered */
    val active: Boolean? = null,
    /* Authorization header to include in webhook requests */
    val authorizationHeader: String? = null,
    /* Branch filter pattern to determine which branches trigger the webhook */
    val branchFilter: String? = null,
    /* Configuration settings for the webhook */
    val config: Map<String, String>? = null,
    /* The date and time when the webhook was created */
    val createdAt: OffsetDateTime? = null,
    /* List of events that trigger this webhook */
    val events: Array<String>? = null,
    /* The unique identifier of the webhook */
    val id: Long? = null,
    /* Optional human-readable name for the webhook */
    val name: String? = null,
    /* The type of the webhook (e.g., gitea, slack, discord) */
    val type: String? = null,
    /* The date and time when the webhook was last updated */
    val updatedAt: OffsetDateTime? = null,
)

