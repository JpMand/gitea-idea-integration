package com.github.jpmand.idea.plugin.gitea.api.rest.dto

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

        APPROVED("APPROVED"),

        PENDING("PENDING"),

        COMMENT("COMMENT"),

        REQUESTCHANGES("REQUEST_CHANGES"),

        REQUESTREVIEW("REQUEST_REVIEW");

    }


}

