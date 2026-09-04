package com.github.jpmand.idea.plugin.gitea.api.rest.dto


/**
 * 
 * @param sha The SHA hash of the Git object
 * @param type The type of the Git object (e.g., commit, tag, tree, blob)
 * @param url The URL to access this Git object
 */
data class GitObject(
    /* The SHA hash of the Git object */
    val sha: String? = null,
    /* The type of the Git object (e.g., commit, tag, tree, blob) */
    val type: String? = null,
    /* The URL to access this Git object */
    val url: String? = null,
)

