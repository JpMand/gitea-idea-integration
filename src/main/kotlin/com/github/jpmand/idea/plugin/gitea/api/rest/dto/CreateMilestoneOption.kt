package com.github.jpmand.idea.plugin.gitea.api.rest.dto

import java.time.OffsetDateTime


/**
 * CreateMilestoneOption options for creating a milestone
 * @param description Description provides details about the milestone
 * @param dueOn Deadline is the due date for the milestone
 * @param state State indicates the initial state of the milestone
 * @param title Title is the title of the new milestone
 */
data class CreateMilestoneOption(
    /* Description provides details about the milestone */
    val description: String? = null,
    /* Deadline is the due date for the milestone */
    val dueOn: OffsetDateTime? = null,
    /* State indicates the initial state of the milestone */
    val state: State? = null,
    /* Title is the title of the new milestone */
    val title: String? = null,
) {


    /**
     * State indicates the initial state of the milestone
     * Values: OPEN,CLOSED
     */
    enum class State(val value: String) {

        OPEN("open"),

        CLOSED("closed");

    }


}

