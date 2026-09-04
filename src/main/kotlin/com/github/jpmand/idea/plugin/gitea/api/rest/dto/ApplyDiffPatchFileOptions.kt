package com.github.jpmand.idea.plugin.gitea.api.rest.dto

/**
 * ApplyDiffPatchFileOptions options for applying a diff patch Note: `author` and `committer` are optional (if only one is given, it will be used for the other, otherwise the authenticated user will be used)
 * @param author
 * @param branch branch (optional) is the base branch for the changes. If not supplied, the default branch is used
 * @param committer
 * @param content
 * @param dates
 * @param forcePush force_push (optional) will do a force-push if the new branch already exists
 * @param message message (optional) is the commit message of the changes. If not supplied, a default message will be used
 * @param newBranch new_branch (optional) will make a new branch from base branch for the changes. If not supplied, the changes will be committed to the base branch
 * @param signoff Add a Signed-off-by trailer by the committer at the end of the commit log message.
 */
data class ApplyDiffPatchFileOptions(
    val author: Identity? = null,
    /* branch (optional) is the base branch for the changes. If not supplied, the default branch is used */
    val branch: String? = null,
    val committer: Identity? = null,
    val content: String,
    val dates: CommitDateOptions? = null,
    /* force_push (optional) will do a force-push if the new branch already exists */
    val forcePush: Boolean? = null,
    /* message (optional) is the commit message of the changes. If not supplied, a default message will be used */
    val message: String? = null,
    /* new_branch (optional) will make a new branch from base branch for the changes. If not supplied, the changes will be committed to the base branch */
    val newBranch: String? = null,
    /* Add a Signed-off-by trailer by the committer at the end of the commit log message. */
    val signoff: Boolean? = null,
)

