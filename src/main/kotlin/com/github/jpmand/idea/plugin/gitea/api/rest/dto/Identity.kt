package com.github.jpmand.idea.plugin.gitea.api.rest.dto


/**
 * Identity for a person's identity like an author or committer
 * @param email Email is the person's email address
 * @param name Name is the person's name
 */
data class Identity(
    /* Email is the person's email address */
    val email: String? = null,
    /* Name is the person's name */
    val name: String? = null,
)

