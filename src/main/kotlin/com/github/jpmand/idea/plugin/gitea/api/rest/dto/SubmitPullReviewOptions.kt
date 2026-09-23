package com.github.jpmand.idea.plugin.gitea.api.rest.dto

import com.fasterxml.jackson.annotation.JsonProperty


/**
 * SubmitPullReviewOptions are options to submit a pending pull request review
 * @param body
 * @param event
 */
data class SubmitPullReviewOptions(
    val body: String? = null,
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

