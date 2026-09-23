package com.github.jpmand.idea.plugin.gitea.api.rest.dto


/**
 * CreateKeyOption options when creating a key
 * @param key An armored SSH key to add
 * @param readOnly Describe if the key has only read access or read/write
 * @param title Title of the key to add
 */
data class CreateKeyOption(
    /* An armored SSH key to add */
    val key: String,
    /* Describe if the key has only read access or read/write */
    val readOnly: Boolean? = null,
    /* Title of the key to add */
    val title: String,
)

