package com.github.jpmand.idea.plugin.gitea.api.rest.dto


/**
 * CreateOrUpdateSecretOption options when creating or updating secret
 * @param &#x60;data&#x60; Data of the secret to update
 * @param description Description of the secret to update
 */
data class CreateOrUpdateSecretOption(
    /* Data of the secret to update */
    val `data`: String,
    /* Description of the secret to update */
    val description: String? = null,
)

