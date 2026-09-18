package com.github.jpmand.idea.plugin.gitea.api.rest.dto


/**
 * CreateOAuth2ApplicationOptions holds options to create an oauth2 application
 * @param confidentialClient Whether the client is confidential
 * @param name The name of the OAuth2 application
 * @param redirectUris The list of allowed redirect URIs
 * @param skipSecondaryAuthorization Whether to skip secondary authorization
 */
data class CreateOAuth2ApplicationOptions(
    /* Whether the client is confidential */
    val confidentialClient: Boolean? = null,
    /* The name of the OAuth2 application */
    val name: String? = null,
    /* The list of allowed redirect URIs */
    val redirectUris: Array<String>? = null,
    /* Whether to skip secondary authorization */
    val skipSecondaryAuthorization: Boolean? = null,
)

