package com.github.jpmand.idea.plugin.gitea.api.rest.dto

import java.time.OffsetDateTime


/**
 * Cron represents a Cron task
 * @param execTimes The total number of times this cron task has been executed
 * @param name The name of the cron task
 * @param next The next scheduled execution time
 * @param prev The previous execution time
 * @param schedule The cron schedule expression (e.g., \"0 0 * * *\")
 */
data class Cron(
    /* The total number of times this cron task has been executed */
    val execTimes: Long? = null,
    /* The name of the cron task */
    val name: String? = null,
    /* The next scheduled execution time */
    val next: OffsetDateTime? = null,
    /* The previous execution time */
    val prev: OffsetDateTime? = null,
    /* The cron schedule expression (e.g., \"0 0 * * *\") */
    val schedule: String? = null,
)

