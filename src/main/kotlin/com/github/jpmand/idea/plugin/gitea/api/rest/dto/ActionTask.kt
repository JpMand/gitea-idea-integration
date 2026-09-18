package com.github.jpmand.idea.plugin.gitea.api.rest.dto

import java.time.OffsetDateTime


/**
 * ActionTask represents a ActionTask
 * @param createdAt
 * @param displayTitle DisplayTitle is the display title for the workflow run
 * @param event Event is the type of event that triggered the workflow
 * @param headBranch HeadBranch is the branch that triggered the workflow
 * @param headSha HeadSHA is the commit SHA that triggered the workflow
 * @param id ID is the unique identifier for the action task
 * @param name Name is the name of the workflow
 * @param runNumber RunNumber is the sequential number of the workflow run
 * @param runStartedAt
 * @param status Status indicates the current status of the workflow run
 * @param updatedAt
 * @param url URL is the API URL for this workflow run
 * @param workflowId WorkflowID is the identifier of the workflow
 */
data class ActionTask(
    val createdAt: OffsetDateTime? = null,
    /* DisplayTitle is the display title for the workflow run */
    val displayTitle: String? = null,
    /* Event is the type of event that triggered the workflow */
    val event: String? = null,
    /* HeadBranch is the branch that triggered the workflow */
    val headBranch: String? = null,
    /* HeadSHA is the commit SHA that triggered the workflow */
    val headSha: String? = null,
    /* ID is the unique identifier for the action task */
    val id: Long? = null,
    /* Name is the name of the workflow */
    val name: String? = null,
    /* RunNumber is the sequential number of the workflow run */
    val runNumber: Long? = null,
    val runStartedAt: OffsetDateTime? = null,
    /* Status indicates the current status of the workflow run */
    val status: String? = null,
    val updatedAt: OffsetDateTime? = null,
    /* URL is the API URL for this workflow run */
    val url: String? = null,
    /* WorkflowID is the identifier of the workflow */
    val workflowId: String? = null,
)

