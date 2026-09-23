package com.github.jpmand.idea.plugin.gitea.api.rest.dto

/**
 * PRBranchInfo information about a branch
 * @param label The display name of the branch
 * @param ref The git reference of the branch
 * @param repo
 * @param repoId The unique identifier of the repository
 * @param sha The commit SHA of the branch head
 */
data class PRBranchInfo(
    /* The display name of the branch */
    val label: String? = null,
    /* The git reference of the branch */
    val ref: String? = null,
    val repo: Repository? = null,
    /* The unique identifier of the repository */
    val repoId: Long? = null,
    /* The commit SHA of the branch head */
    val sha: String? = null,
)

