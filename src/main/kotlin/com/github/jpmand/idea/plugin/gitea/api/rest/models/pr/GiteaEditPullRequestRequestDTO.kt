package com.github.jpmand.idea.plugin.gitea.api.rest.models.pr

/** Request body for PATCH /repos/{owner}/{repo}/pulls/{index} (EditPullRequestOption). */
class GiteaEditPullRequestRequestDTO(
    val title: String? = null,
    val body: String? = null,
    /** "open" or "closed" */
    val state: String? = null,
    val assignee: String? = null,
    val assignees: List<String>? = null,
    val labels: List<Int>? = null,
    val milestone: Long? = null,
    val allowMaintainerEdit: Boolean? = null,
    /** Rename the target branch of the PR. */
    val base: String? = null,
)
