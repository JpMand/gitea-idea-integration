package com.github.jpmand.idea.plugin.gitea.api.rest.dto

/**
 * ActionRunner represents a Runner
 * @param busy
 * @param disabled
 * @param ephemeral
 * @param id
 * @param labels
 * @param name
 * @param status
 */
data class ActionRunner(
    val busy: Boolean? = null,
    val disabled: Boolean? = null,
    val ephemeral: Boolean? = null,
    val id: Long? = null,
    val labels: Array<ActionRunnerLabel>? = null,
    val name: String? = null,
    val status: String? = null,
)

