package com.github.jpmand.idea.plugin.gitea.api.rest.dto


/**
 * CreateActionWorkflowDispatch represents the payload for triggering a workflow dispatch event
 * @param inputs
 * @param ref
 */
data class CreateActionWorkflowDispatch(
    val inputs: Map<String, String>? = null,
    val ref: String,
)

