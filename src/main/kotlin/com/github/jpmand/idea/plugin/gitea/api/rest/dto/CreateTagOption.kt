package com.github.jpmand.idea.plugin.gitea.api.rest.dto


/**
 * CreateTagOption options when creating a tag
 * @param message The message to associate with the tag
 * @param tagName The name of the tag to create
 * @param target The target commit SHA or branch name for the tag
 */
data class CreateTagOption(
    /* The message to associate with the tag */
    val message: String? = null,
    /* The name of the tag to create */
    val tagName: String,
    /* The target commit SHA or branch name for the tag */
    val target: String? = null,
)

