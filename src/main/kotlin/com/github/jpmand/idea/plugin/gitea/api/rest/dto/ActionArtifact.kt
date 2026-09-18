package com.github.jpmand.idea.plugin.gitea.api.rest.dto

import java.time.OffsetDateTime

/**
 * ActionArtifact represents a ActionArtifact
 * @param archiveDownloadUrl
 * @param createdAt
 * @param expired
 * @param expiresAt
 * @param id
 * @param name
 * @param sizeInBytes
 * @param updatedAt
 * @param url
 * @param workflowRun
 */
data class ActionArtifact(
    val archiveDownloadUrl: String? = null,
    val createdAt: OffsetDateTime? = null,
    val expired: Boolean? = null,
    val expiresAt: OffsetDateTime? = null,
    val id: Long? = null,
    val name: String? = null,
    val sizeInBytes: Long? = null,
    val updatedAt: OffsetDateTime? = null,
    val url: String? = null,
    val workflowRun: ActionWorkflowRun? = null,
)

