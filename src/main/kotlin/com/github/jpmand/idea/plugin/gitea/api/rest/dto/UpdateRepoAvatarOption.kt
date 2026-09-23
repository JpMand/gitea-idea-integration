package com.github.jpmand.idea.plugin.gitea.api.rest.dto


/**
 * UpdateRepoAvatarUserOption options when updating the repo avatar
 * @param image image must be base64 encoded
 */
data class UpdateRepoAvatarOption(
    /* image must be base64 encoded */
    val image: String? = null,
)

