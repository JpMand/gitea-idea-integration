package com.github.jpmand.idea.plugin.gitea.api.rest.dto

/**
 * CombinedStatus holds the combined state of several statuses for a single commit
 * @param commitUrl CommitURL is the API URL for the commit
 * @param repository
 * @param sha SHA is the commit SHA this status applies to
 * @param state State is the overall combined status state
 * @param statuses Statuses contains all individual commit statuses
 * @param totalCount TotalCount is the total number of statuses
 * @param url URL is the API URL for this combined status
 */
data class CombinedStatus(
    /* CommitURL is the API URL for the commit */
    val commitUrl: String? = null,
    val repository: Repository? = null,
    /* SHA is the commit SHA this status applies to */
    val sha: String? = null,
    /* State is the overall combined status state */
    val state: State? = null,
    /* Statuses contains all individual commit statuses */
    val statuses: Array<CommitStatus>? = null,
    /* TotalCount is the total number of statuses */
    val totalCount: Long? = null,
    /* URL is the API URL for this combined status */
    val url: String? = null,
) {


    /**
     * State is the overall combined status state
     * Values: PENDING,SUCCESS,ERROR,FAILURE,WARNING,SKIPPED
     */
    enum class State(val value: String) {

        PENDING("pending"),

        SUCCESS("success"),

        ERROR("error"),

        FAILURE("failure"),

        WARNING("warning"),

        SKIPPED("skipped");

    }


}

