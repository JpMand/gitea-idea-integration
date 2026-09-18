package com.github.jpmand.idea.plugin.gitea.api.rest.dto

import java.time.OffsetDateTime


/**
 * 
 * @param clientId The client ID of the OAuth2 application
 * @param clientSecret The client secret of the OAuth2 application
 * @param confidentialClient Whether the client is confidential
 * @param created The timestamp when the application was created
 * @param id The unique identifier of the OAuth2 application
 * @param name The name of the OAuth2 application
 * @param redirectUris The list of allowed redirect URIs
 * @param skipSecondaryAuthorization Whether to skip secondary authorization
 */
data class OAuth2Application(
    /* The client ID of the OAuth2 application */
    val clientId: String? = null,
    /* The client secret of the OAuth2 application */
    val clientSecret: String? = null,
    /* Whether the client is confidential */
    val confidentialClient: Boolean? = null,
    /* The timestamp when the application was created */
    val created: OffsetDateTime? = null,
    /* The unique identifier of the OAuth2 application */
    val id: Long? = null,
    /* The name of the OAuth2 application */
    val name: String? = null,
    /* The list of allowed redirect URIs */
    val redirectUris: Array<String>? = null,
    /* Whether to skip secondary authorization */
    val skipSecondaryAuthorization: Boolean? = null,
)

