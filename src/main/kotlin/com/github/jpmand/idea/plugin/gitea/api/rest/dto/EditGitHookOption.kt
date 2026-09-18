package com.github.jpmand.idea.plugin.gitea.api.rest.dto


/**
 * EditGitHookOption options when modifying one Git hook
 * @param content Content is the new script content for the hook
 */
data class EditGitHookOption(
    /* Content is the new script content for the hook */
    val content: String? = null,
)

