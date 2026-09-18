package com.github.jpmand.idea.plugin.gitea.api.rest.dto


/**
 * PayloadUser represents the author or committer of a commit
 * @param email
 * @param name Full name of the commit author
 * @param username username of the user
 */
data class PayloadUser(
    val email: String? = null,
    /* Full name of the commit author */
    val name: String? = null,
    /* username of the user */
    val username: String? = null,
)

