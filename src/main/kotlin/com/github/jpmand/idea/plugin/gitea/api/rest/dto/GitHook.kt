package com.github.jpmand.idea.plugin.gitea.api.rest.dto


/**
 * GitHook represents a Git repository hook
 * @param content Content contains the script content of the hook
 * @param isActive IsActive indicates if the hook is active
 * @param name Name is the name of the Git hook
 */
data class GitHook(
    /* Content contains the script content of the hook */
    val content: String? = null,
    /* IsActive indicates if the hook is active */
    val isActive: Boolean? = null,
    /* Name is the name of the Git hook */
    val name: String? = null,
)

