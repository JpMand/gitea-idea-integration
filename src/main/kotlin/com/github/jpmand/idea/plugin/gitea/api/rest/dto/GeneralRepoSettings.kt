package com.github.jpmand.idea.plugin.gitea.api.rest.dto


/**
 * GeneralRepoSettings contains global repository settings exposed by API
 * @param httpGitDisabled HTTPGitDisabled indicates if HTTP Git operations are disabled
 * @param lfsDisabled LFSDisabled indicates if Git LFS support is disabled
 * @param migrationsDisabled MigrationsDisabled indicates if repository migrations are disabled
 * @param mirrorsDisabled MirrorsDisabled indicates if repository mirroring is disabled
 * @param starsDisabled StarsDisabled indicates if repository starring is disabled
 * @param timeTrackingDisabled TimeTrackingDisabled indicates if time tracking is disabled
 */
data class GeneralRepoSettings(
    /* HTTPGitDisabled indicates if HTTP Git operations are disabled */
    val httpGitDisabled: Boolean? = null,
    /* LFSDisabled indicates if Git LFS support is disabled */
    val lfsDisabled: Boolean? = null,
    /* MigrationsDisabled indicates if repository migrations are disabled */
    val migrationsDisabled: Boolean? = null,
    /* MirrorsDisabled indicates if repository mirroring is disabled */
    val mirrorsDisabled: Boolean? = null,
    /* StarsDisabled indicates if repository starring is disabled */
    val starsDisabled: Boolean? = null,
    /* TimeTrackingDisabled indicates if time tracking is disabled */
    val timeTrackingDisabled: Boolean? = null,
)

