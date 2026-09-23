package com.github.jpmand.idea.plugin.gitea.api.rest.dto

/**
 * Response body of `GET /users/search` — not a plain array like most other list endpoints, so it
 * gets its own wrapper DTO instead of reusing [com.intellij.collaboration.api.json.loadJsonList].
 */
data class UserSearchResult(
    val data: List<User>? = null,
    val ok: Boolean? = null,
)
