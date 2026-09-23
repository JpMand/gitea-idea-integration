package com.github.jpmand.idea.plugin.gitea.api.rest.dto

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * CreatePullReviewOptions are options to create a pull request review
 * @param body
 * @param comments
 * @param commitId
 * @param event
 */
data class CreatePullReviewOptions(
    val body: String? = null,
    val comments: Array<CreatePullReviewComment>? = null,
    val commitId: String? = null,
    val event: Event? = null,
) {


    /**
     *
     * Values: APPROVED,PENDING,COMMENT,REQUESTCHANGES,REQUESTREVIEW
     */
    enum class Event(val value: String) {

        @JsonProperty("APPROVED") APPROVED("APPROVED"),

        @JsonProperty("PENDING") PENDING("PENDING"),

        @JsonProperty("COMMENT") COMMENT("COMMENT"),

        @JsonProperty("REQUEST_CHANGES") REQUESTCHANGES("REQUEST_CHANGES"),

        @JsonProperty("REQUEST_REVIEW") REQUESTREVIEW("REQUEST_REVIEW");

    }


}

