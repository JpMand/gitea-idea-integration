package com.github.jpmand.idea.plugin.gitea.api.rest.dto


/**
 * AnnotatedTagObject contains meta information of the tag object
 * @param sha The SHA hash of the tagged object
 * @param type The type of the tagged object (e.g., commit, tree)
 * @param url The URL to access the tagged object
 */
data class AnnotatedTagObject(
    /* The SHA hash of the tagged object */
    val sha: String? = null,
    /* The type of the tagged object (e.g., commit, tree) */
    val type: String? = null,
    /* The URL to access the tagged object */
    val url: String? = null,
)

