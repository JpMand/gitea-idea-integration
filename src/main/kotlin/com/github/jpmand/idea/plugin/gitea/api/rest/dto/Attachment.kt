package com.github.jpmand.idea.plugin.gitea.api.rest.dto

import java.time.OffsetDateTime


/**
 * Attachment a generic attachment
 * @param browserDownloadUrl DownloadURL is the URL to download the attachment
 * @param createdAt Created is the time when the attachment was uploaded
 * @param downloadCount DownloadCount is the number of times the attachment has been downloaded
 * @param id ID is the unique identifier for the attachment
 * @param name Name is the filename of the attachment
 * @param size Size is the file size in bytes
 * @param uuid UUID is the unique identifier for the attachment file
 */
data class Attachment(
    /* DownloadURL is the URL to download the attachment */
    val browserDownloadUrl: String? = null,
    /* Created is the time when the attachment was uploaded */
    val createdAt: OffsetDateTime? = null,
    /* DownloadCount is the number of times the attachment has been downloaded */
    val downloadCount: Long? = null,
    /* ID is the unique identifier for the attachment */
    val id: Long? = null,
    /* Name is the filename of the attachment */
    val name: String? = null,
    /* Size is the file size in bytes */
    val size: Long? = null,
    /* UUID is the unique identifier for the attachment file */
    val uuid: String? = null,
)

