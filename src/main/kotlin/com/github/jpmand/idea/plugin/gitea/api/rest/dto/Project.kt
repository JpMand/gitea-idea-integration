package com.github.jpmand.idea.plugin.gitea.api.rest.dto

import java.time.OffsetDateTime


/**
 * Project represents a project
 * @param closedAt
 * @param createdAt
 * @param creatorId CreatorID is the user who created the project
 * @param description Description provides details about the project
 * @param id ID is the unique identifier for the project
 * @param isClosed IsClosed indicates if the project is closed
 * @param ownerId OwnerID is the owner of the project (for org-level projects)
 * @param repoId RepoID is the repository this project belongs to (for repo-level projects)
 * @param title Title is the title of the project
 * @param updatedAt
 */
data class Project(
    val closedAt: OffsetDateTime? = null,
    val createdAt: OffsetDateTime? = null,
    /* CreatorID is the user who created the project */
    val creatorId: Long? = null,
    /* Description provides details about the project */
    val description: String? = null,
    /* ID is the unique identifier for the project */
    val id: Long? = null,
    /* IsClosed indicates if the project is closed */
    val isClosed: Boolean? = null,
    /* OwnerID is the owner of the project (for org-level projects) */
    val ownerId: Long? = null,
    /* RepoID is the repository this project belongs to (for repo-level projects) */
    val repoId: Long? = null,
    /* Title is the title of the project */
    val title: String? = null,
    val updatedAt: OffsetDateTime? = null,
)

