package com.github.jpmand.idea.plugin.gitea.api.rest.dto

import java.time.OffsetDateTime


/**
 * EditMilestoneOption options for editing a milestone
 * @param description Description provides updated details about the milestone
 * @param dueOn Deadline is the updated due date for the milestone
 * @param state State indicates the updated state of the milestone
 * @param title Title is the updated title of the milestone
 */
data class EditMilestoneOption(
    /* Description provides updated details about the milestone */
    val description: String? = null,
    /* Deadline is the updated due date for the milestone */
    val dueOn: OffsetDateTime? = null,
    /* State indicates the updated state of the milestone */
    val state: State? = null,
    /* Title is the updated title of the milestone */
    val title: String? = null,
) {


    /**
     * State indicates the updated state of the milestone
     * Values: OPEN,CLOSED
     */
    enum class State(val value: String) {

        OPEN("open"),

        CLOSED("closed");

    }


}

