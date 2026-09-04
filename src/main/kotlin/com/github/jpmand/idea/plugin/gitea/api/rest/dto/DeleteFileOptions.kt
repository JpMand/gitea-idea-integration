package com.github.jpmand.idea.plugin.gitea.api.rest.dto

/**
 * DeleteFileOptions options for deleting a file Note: `author` and `committer` are optional (if only one is given, it will be used for the other, otherwise the authenticated user will be used)
 * @param author
 * @param branch branch (optional) is the base branch for the changes. If not supplied, the default branch is used
 * @param committer
 * @param dates
 * @param forcePush force_push (optional) will do a force-push if the new branch already exists
 * @param message message (optional) is the commit message of the changes. If not supplied, a default message will be used
 * @param newBranch new_branch (optional) will make a new branch from base branch for the changes. If not supplied, the changes will be committed to the base branch
 * @param sha the blob ID (SHA) for the file to delete
 * @param signoff Add a Signed-off-by trailer by the committer at the end of the commit log message.
 */
data class DeleteFileOptions(
    val author: Identity? = null,
    /* branch (optional) is the base branch for the changes. If not supplied, the default branch is used */
    val branch: String? = null,
    val committer: Identity? = null,
    val dates: CommitDateOptions? = null,
    /* force_push (optional) will do a force-push if the new branch already exists */
    val forcePush: Boolean? = null,
    /* message (optional) is the commit message of the changes. If not supplied, a default message will be used */
    val message: String? = null,
    /* new_branch (optional) will make a new branch from base branch for the changes. If not supplied, the changes will be committed to the base branch */
    val newBranch: String? = null,
    /* the blob ID (SHA) for the file to delete */
    val sha: String,
    /* Add a Signed-off-by trailer by the committer at the end of the commit log message. */
    val signoff: Boolean? = null,
)

