package com.github.jpmand.idea.plugin.gitea.api.rest.dto


/**
 * GitignoreTemplateInfo name and text of a gitignore template
 * @param name Name is the name of the gitignore template
 * @param source Source contains the content of the gitignore template
 */
data class GitignoreTemplateInfo(
    /* Name is the name of the gitignore template */
    val name: String? = null,
    /* Source contains the content of the gitignore template */
    val source: String? = null,
)

