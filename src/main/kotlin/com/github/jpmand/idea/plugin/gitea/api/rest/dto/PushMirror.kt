package com.github.jpmand.idea.plugin.gitea.api.rest.dto

import java.time.OffsetDateTime


/**
 * PushMirror represents information of a push mirror
 * @param created
 * @param interval The sync interval for automatic updates
 * @param lastError The last error message encountered during sync
 * @param lastUpdate
 * @param remoteAddress The remote repository URL being mirrored to
 * @param remoteName The name of the remote in the git configuration
 * @param repoName The name of the source repository
 * @param syncOnCommit Whether to sync on every commit
 */
data class PushMirror(
    val created: OffsetDateTime? = null,
    /* The sync interval for automatic updates */
    val interval: String? = null,
    /* The last error message encountered during sync */
    val lastError: String? = null,
    val lastUpdate: OffsetDateTime? = null,
    /* The remote repository URL being mirrored to */
    val remoteAddress: String? = null,
    /* The name of the remote in the git configuration */
    val remoteName: String? = null,
    /* The name of the source repository */
    val repoName: String? = null,
    /* Whether to sync on every commit */
    val syncOnCommit: Boolean? = null,
)

