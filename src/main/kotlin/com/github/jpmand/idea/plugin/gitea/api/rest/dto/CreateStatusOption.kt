package com.github.jpmand.idea.plugin.gitea.api.rest.dto


/**
 * CreateStatusOption holds the information needed to create a new CommitStatus for a Commit
 * @param context Context is the unique context identifier for the status
 * @param description Description provides a brief description of the status
 * @param state State represents the status state to set (pending, success, error, failure)
 * @param targetUrl TargetURL is the URL to link to for more details
 */
data class CreateStatusOption(
    /* Context is the unique context identifier for the status */
    val context: String? = null,
    /* Description provides a brief description of the status */
    val description: String? = null,
    /* State represents the status state to set (pending, success, error, failure) */
    val state: State? = null,
    /* TargetURL is the URL to link to for more details */
    val targetUrl: String? = null,
) {


    /**
     * State represents the status state to set (pending, success, error, failure)
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

