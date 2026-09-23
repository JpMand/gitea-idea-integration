package com.github.jpmand.idea.plugin.gitea.api.rest.dto


/**
 * EditHookOption options when modify one hook
 * @param active Whether the webhook is active and will be triggered
 * @param authorizationHeader Authorization header to include in webhook requests
 * @param branchFilter Branch filter pattern to determine which branches trigger the webhook
 * @param config Configuration settings for the webhook
 * @param events List of events that trigger this webhook
 * @param name Optional human-readable name
 */
data class EditHookOption(
    /* Whether the webhook is active and will be triggered */
    val active: Boolean? = null,
    /* Authorization header to include in webhook requests */
    val authorizationHeader: String? = null,
    /* Branch filter pattern to determine which branches trigger the webhook */
    val branchFilter: String? = null,
    /* Configuration settings for the webhook */
    val config: Map<String, String>? = null,
    /* List of events that trigger this webhook */
    val events: Array<String>? = null,
    /* Optional human-readable name */
    val name: String? = null,
)

