package com.github.jpmand.idea.plugin.gitea.api.rest.dto

import java.time.OffsetDateTime


/**
 * StopWatch represent a running stopwatch
 * @param created Created is the time when the stopwatch was started
 * @param duration Duration is a human-readable duration string
 * @param issueIndex IssueIndex is the index number of the associated issue
 * @param issueTitle IssueTitle is the title of the associated issue
 * @param repoName RepoName is the name of the repository
 * @param repoOwnerName RepoOwnerName is the name of the repository owner
 * @param seconds Seconds is the total elapsed time in seconds
 */
data class StopWatch(
    /* Created is the time when the stopwatch was started */
    val created: OffsetDateTime? = null,
    /* Duration is a human-readable duration string */
    val duration: String? = null,
    /* IssueIndex is the index number of the associated issue */
    val issueIndex: Long? = null,
    /* IssueTitle is the title of the associated issue */
    val issueTitle: String? = null,
    /* RepoName is the name of the repository */
    val repoName: String? = null,
    /* RepoOwnerName is the name of the repository owner */
    val repoOwnerName: String? = null,
    /* Seconds is the total elapsed time in seconds */
    val seconds: Long? = null,
)

