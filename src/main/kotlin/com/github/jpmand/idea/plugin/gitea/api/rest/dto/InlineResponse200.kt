package com.github.jpmand.idea.plugin.gitea.api.rest.dto

/**
 * 
 * @param &#x60;data&#x60; 
 * @param ok
 */
data class InlineResponse200(
    val `data`: Array<Team>? = null,
    val ok: Boolean? = null,
)

