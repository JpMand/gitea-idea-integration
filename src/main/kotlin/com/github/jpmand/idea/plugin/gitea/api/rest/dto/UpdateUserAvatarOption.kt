package com.github.jpmand.idea.plugin.gitea.api.rest.dto


/**
 * UpdateUserAvatarUserOption options when updating the user avatar
 * @param image image must be base64 encoded
 */
data class UpdateUserAvatarOption(
    /* image must be base64 encoded */
    val image: String? = null,
)

