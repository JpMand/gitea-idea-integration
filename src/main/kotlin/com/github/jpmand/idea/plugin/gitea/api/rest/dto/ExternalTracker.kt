package com.github.jpmand.idea.plugin.gitea.api.rest.dto


/**
 * ExternalTracker represents settings for external tracker
 * @param externalTrackerFormat External Issue Tracker URL Format. Use the placeholders {user}, {repo} and {index} for the username, repository name and issue index.
 * @param externalTrackerRegexpPattern External Issue Tracker issue regular expression
 * @param externalTrackerStyle External Issue Tracker Number Format, either `numeric`, `alphanumeric`, or `regexp`
 * @param externalTrackerUrl URL of external issue tracker.
 */
data class ExternalTracker(
    /* External Issue Tracker URL Format. Use the placeholders {user}, {repo} and {index} for the username, repository name and issue index. */
    val externalTrackerFormat: String? = null,
    /* External Issue Tracker issue regular expression */
    val externalTrackerRegexpPattern: String? = null,
    /* External Issue Tracker Number Format, either `numeric`, `alphanumeric`, or `regexp` */
    val externalTrackerStyle: String? = null,
    /* URL of external issue tracker. */
    val externalTrackerUrl: String? = null,
)

