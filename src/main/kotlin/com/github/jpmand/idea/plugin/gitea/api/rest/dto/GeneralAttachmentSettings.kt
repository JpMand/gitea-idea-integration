package com.github.jpmand.idea.plugin.gitea.api.rest.dto


/**
 * GeneralAttachmentSettings contains global Attachment settings exposed by API
 * @param allowedTypes AllowedTypes contains the allowed file types for attachments
 * @param enabled Enabled indicates if file attachments are enabled
 * @param maxFiles MaxFiles is the maximum number of files per attachment
 * @param maxSize MaxSize is the maximum size for individual attachments
 */
data class GeneralAttachmentSettings(
    /* AllowedTypes contains the allowed file types for attachments */
    val allowedTypes: String? = null,
    /* Enabled indicates if file attachments are enabled */
    val enabled: Boolean? = null,
    /* MaxFiles is the maximum number of files per attachment */
    val maxFiles: Long? = null,
    /* MaxSize is the maximum size for individual attachments */
    val maxSize: Long? = null,
)

