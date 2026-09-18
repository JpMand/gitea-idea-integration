package com.github.jpmand.idea.plugin.gitea.api.rest.dto

import java.time.OffsetDateTime

/**
 * Issue represents an issue in a repository
 * @param assets
 * @param assignee
 * @param assignees
 * @param body
 * @param closedAt
 * @param comments
 * @param contentVersion The version of the issue content for optimistic locking
 * @param createdAt
 * @param dueDate
 * @param htmlUrl
 * @param id
 * @param isLocked
 * @param labels
 * @param milestone
 * @param number
 * @param originalAuthor
 * @param originalAuthorId
 * @param pinOrder
 * @param projects
 * @param pullRequest
 * @param ref
 * @param repository
 * @param state
 * @param timeEstimate
 * @param title
 * @param updatedAt
 * @param url
 * @param user
 */
data class Issue(
    val assets: Array<Attachment>? = null,
    val assignee: User? = null,
    val assignees: Array<User>? = null,
    val body: String? = null,
    val closedAt: OffsetDateTime? = null,
    val comments: Long? = null,
    /* The version of the issue content for optimistic locking */
    val contentVersion: Long? = null,
    val createdAt: OffsetDateTime? = null,
    val dueDate: OffsetDateTime? = null,
    val htmlUrl: String? = null,
    val id: Long? = null,
    val isLocked: Boolean? = null,
    val labels: Array<Label>? = null,
    val milestone: Milestone? = null,
    val number: Long? = null,
    val originalAuthor: String? = null,
    val originalAuthorId: Long? = null,
    val pinOrder: Long? = null,
    val projects: Array<Project>? = null,
    val pullRequest: PullRequestMeta? = null,
    val ref: String? = null,
    val repository: RepositoryMeta? = null,
    val state: State? = null,
    val timeEstimate: Long? = null,
    val title: String? = null,
    val updatedAt: OffsetDateTime? = null,
    val url: String? = null,
    val user: User? = null,
) {


    /**
     *
     * Values: OPEN,CLOSED
     */
    enum class State(val value: String) {

        OPEN("open"),

        CLOSED("closed");

    }


}

