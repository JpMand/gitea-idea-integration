package com.github.jpmand.idea.plugin.gitea.api.rest.dto

import java.time.OffsetDateTime


/**
 * Milestone milestone is a collection of issues on one repository
 * @param closedAt
 * @param closedIssues ClosedIssues is the number of closed issues in this milestone
 * @param createdAt
 * @param description Description provides details about the milestone
 * @param dueOn
 * @param id ID is the unique identifier for the milestone
 * @param openIssues OpenIssues is the number of open issues in this milestone
 * @param state State indicates if the milestone is open or closed
 * @param title Title is the title of the milestone
 * @param updatedAt
 */
data class Milestone(
    val closedAt: OffsetDateTime? = null,
    /* ClosedIssues is the number of closed issues in this milestone */
    val closedIssues: Long? = null,
    val createdAt: OffsetDateTime? = null,
    /* Description provides details about the milestone */
    val description: String? = null,
    val dueOn: OffsetDateTime? = null,
    /* ID is the unique identifier for the milestone */
    val id: Long? = null,
    /* OpenIssues is the number of open issues in this milestone */
    val openIssues: Long? = null,
    /* State indicates if the milestone is open or closed */
    val state: State? = null,
    /* Title is the title of the milestone */
    val title: String? = null,
    val updatedAt: OffsetDateTime? = null,
) {


    /**
     * State indicates if the milestone is open or closed
     * Values: OPEN,CLOSED
     */
    enum class State(val value: String) {

        OPEN("open"),

        CLOSED("closed");

    }


}

