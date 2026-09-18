package com.github.jpmand.idea.plugin.gitea.api.rest.dto

/**
 * CreateHookOption options when create a hook
 * @param active Whether the webhook should be active upon creation
 * @param authorizationHeader Authorization header to include in webhook requests
 * @param branchFilter Branch filter pattern to determine which branches trigger the webhook
 * @param config
 * @param events List of events that will trigger this webhook
 * @param name Optional human-readable name for the webhook
 * @param type The type of the webhook to create
 */
data class CreateHookOption(
    /* Whether the webhook should be active upon creation */
    val active: Boolean? = null,
    /* Authorization header to include in webhook requests */
    val authorizationHeader: String? = null,
    /* Branch filter pattern to determine which branches trigger the webhook */
    val branchFilter: String? = null,
    val config: CreateHookOptionConfig,
    /* List of events that will trigger this webhook */
    val events: Array<String>? = null,
    /* Optional human-readable name for the webhook */
    val name: String? = null,
    /* The type of the webhook to create */
    val type: Type,
) {


    /**
     * The type of the webhook to create
     * Values: DINGTALK,DISCORD,GITEA,GOGS,MSTEAMS,SLACK,TELEGRAM,FEISHU,WECHATWORK,PACKAGIST
     */
    enum class Type(val value: String) {

        DINGTALK("dingtalk"),

        DISCORD("discord"),

        GITEA("gitea"),

        GOGS("gogs"),

        MSTEAMS("msteams"),

        SLACK("slack"),

        TELEGRAM("telegram"),

        FEISHU("feishu"),

        WECHATWORK("wechatwork"),

        PACKAGIST("packagist");

    }


}

