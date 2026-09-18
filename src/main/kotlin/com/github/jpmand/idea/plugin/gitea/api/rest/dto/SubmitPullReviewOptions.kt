package com.github.jpmand.idea.plugin.gitea.api.rest.dto


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

        APPROVED("APPROVED"),

        PENDING("PENDING"),

        COMMENT("COMMENT"),

        REQUESTCHANGES("REQUEST_CHANGES"),

        REQUESTREVIEW("REQUEST_REVIEW");

    }


}

