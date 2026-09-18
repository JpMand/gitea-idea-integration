package com.github.jpmand.idea.plugin.gitea.api.rest.dto


/**
 * ActionRunnerLabel represents a Runner Label
 * @param id
 * @param name
 * @param type
 */
data class ActionRunnerLabel(
    val id: Long? = null,
    val name: String? = null,
    val type: String? = null,
)

