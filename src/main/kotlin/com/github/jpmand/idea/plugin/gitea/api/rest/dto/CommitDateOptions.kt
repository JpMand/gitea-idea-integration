package com.github.jpmand.idea.plugin.gitea.api.rest.dto

import java.time.OffsetDateTime


/**
 * CommitDateOptions store dates for GIT_AUTHOR_DATE and GIT_COMMITTER_DATE
 * @param author Author is the author date for the commit
 * @param committer Committer is the committer date for the commit
 */
data class CommitDateOptions(
    /* Author is the author date for the commit */
    val author: OffsetDateTime? = null,
    /* Committer is the committer date for the commit */
    val committer: OffsetDateTime? = null,
)

