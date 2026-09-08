package com.github.jpmand.idea.plugin.gitea.api.rest.dto


/**
 * InternalTracker represents settings for internal tracker
 * @param allowOnlyContributorsToTrackTime Let only contributors track time (Built-in issue tracker)
 * @param enableIssueDependencies Enable dependencies for issues and pull requests (Built-in issue tracker)
 * @param enableTimeTracker Enable time tracking (Built-in issue tracker)
 */
data class InternalTracker(
    /* Let only contributors track time (Built-in issue tracker) */
    val allowOnlyContributorsToTrackTime: Boolean? = null,
    /* Enable dependencies for issues and pull requests (Built-in issue tracker) */
    val enableIssueDependencies: Boolean? = null,
    /* Enable time tracking (Built-in issue tracker) */
    val enableTimeTracker: Boolean? = null,
)

