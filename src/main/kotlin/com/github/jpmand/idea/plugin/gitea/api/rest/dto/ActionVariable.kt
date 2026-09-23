package com.github.jpmand.idea.plugin.gitea.api.rest.dto


/**
 * ActionVariable return value of the query API
 * @param &#x60;data&#x60; the value of the variable
 * @param description the description of the variable
 * @param name the name of the variable
 * @param ownerId the owner to which the variable belongs
 * @param repoId the repository to which the variable belongs
 */
data class ActionVariable(
    /* the value of the variable */
    val `data`: String? = null,
    /* the description of the variable */
    val description: String? = null,
    /* the name of the variable */
    val name: String? = null,
    /* the owner to which the variable belongs */
    val ownerId: Long? = null,
    /* the repository to which the variable belongs */
    val repoId: Long? = null,
)

