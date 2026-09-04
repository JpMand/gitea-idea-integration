package com.github.jpmand.idea.plugin.gitea.api.rest.dto

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue
import java.time.OffsetDateTime

/**
 * PullReview represents a pull request review
 * @param body
 * @param commentsCount
 * @param commitId
 * @param dismissed
 * @param htmlUrl HTMLURL is the web URL for viewing the review
 * @param id
 * @param official
 * @param pullRequestUrl HTMLPullURL is the web URL for the pull request
 * @param stale
 * @param state
 * @param submittedAt
 * @param team
 * @param updatedAt
 * @param user
 */
data class PullReview(
    val body: String? = null,
    val commentsCount: Long? = null,
    val commitId: String? = null,
    val dismissed: Boolean? = null,
    /* HTMLURL is the web URL for viewing the review */
    val htmlUrl: String? = null,
    val id: Long? = null,
    val official: Boolean? = null,
    /* HTMLPullURL is the web URL for the pull request */
    val pullRequestUrl: String? = null,
    val stale: Boolean? = null,
    val state: State? = null,
    val submittedAt: OffsetDateTime? = null,
    val team: Team? = null,
    val updatedAt: OffsetDateTime? = null,
    val user: User? = null,
) {


    /**
     *
     * Values: APPROVED,PENDING,COMMENT,REQUESTCHANGES,REQUESTREVIEW
     */
    enum class State(val value: String) {

        APPROVED("APPROVED"),

        @JsonEnumDefaultValue
        PENDING("PENDING"),

        COMMENT("COMMENT"),

        REQUESTCHANGES("REQUEST_CHANGES"),

        REQUESTREVIEW("REQUEST_REVIEW");

    }


}

