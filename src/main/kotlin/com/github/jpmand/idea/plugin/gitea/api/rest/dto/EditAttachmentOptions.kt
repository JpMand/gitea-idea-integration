package com.github.jpmand.idea.plugin.gitea.api.rest.dto


/**
 * EditAttachmentOptions options for editing attachments
 * @param name Name is the new filename for the attachment
 */
data class EditAttachmentOptions(
    /* Name is the new filename for the attachment */
    val name: String? = null,
)

