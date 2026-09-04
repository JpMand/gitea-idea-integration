package com.github.jpmand.idea.plugin.gitea.api.rest.dto


/**
 * 
 * @param interval The sync interval for automatic updates
 * @param remoteAddress The remote repository URL to push to
 * @param remotePassword The password for authentication with the remote repository
 * @param remoteUsername The username for authentication with the remote repository
 * @param syncOnCommit Whether to sync on every commit
 */
data class CreatePushMirrorOption(
    /* The sync interval for automatic updates */
    val interval: String? = null,
    /* The remote repository URL to push to */
    val remoteAddress: String? = null,
    /* The password for authentication with the remote repository */
    val remotePassword: String? = null,
    /* The username for authentication with the remote repository */
    val remoteUsername: String? = null,
    /* Whether to sync on every commit */
    val syncOnCommit: Boolean? = null,
)

