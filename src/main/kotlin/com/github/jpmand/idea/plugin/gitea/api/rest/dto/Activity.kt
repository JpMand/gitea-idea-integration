package com.github.jpmand.idea.plugin.gitea.api.rest.dto

import java.time.OffsetDateTime

/**
 * 
 * @param actUser
 * @param actUserId The ID of the user who performed the action
 * @param comment
 * @param commentId The ID of the comment associated with the activity (if applicable)
 * @param content Additional content or details about the activity
 * @param created The date and time when the activity occurred
 * @param id The unique identifier of the activity
 * @param isPrivate Whether this activity is from a private repository
 * @param opType the type of action
 * @param refName The name of the git reference (branch/tag) associated with the activity
 * @param repo
 * @param repoId The ID of the repository associated with the activity
 * @param userId The ID of the user who receives/sees this activity
 */
data class Activity(
    val actUser: User? = null,
    /* The ID of the user who performed the action */
    val actUserId: Long? = null,
    val comment: Comment? = null,
    /* The ID of the comment associated with the activity (if applicable) */
    val commentId: Long? = null,
    /* Additional content or details about the activity */
    val content: String? = null,
    /* The date and time when the activity occurred */
    val created: OffsetDateTime? = null,
    /* The unique identifier of the activity */
    val id: Long? = null,
    /* Whether this activity is from a private repository */
    val isPrivate: Boolean? = null,
    /* the type of action */
    val opType: OpType? = null,
    /* The name of the git reference (branch/tag) associated with the activity */
    val refName: String? = null,
    val repo: Repository? = null,
    /* The ID of the repository associated with the activity */
    val repoId: Long? = null,
    /* The ID of the user who receives/sees this activity */
    val userId: Long? = null,
) {


    /**
     * the type of action
     * Values: CREATEREPO,RENAMEREPO,STARREPO,WATCHREPO,COMMITREPO,CREATEISSUE,CREATEPULLREQUEST,TRANSFERREPO,PUSHTAG,COMMENTISSUE,MERGEPULLREQUEST,CLOSEISSUE,REOPENISSUE,CLOSEPULLREQUEST,REOPENPULLREQUEST,DELETETAG,DELETEBRANCH,MIRRORSYNCPUSH,MIRRORSYNCCREATE,MIRRORSYNCDELETE,APPROVEPULLREQUEST,REJECTPULLREQUEST,COMMENTPULL,PUBLISHRELEASE,PULLREVIEWDISMISSED,PULLREQUESTREADYFORREVIEW,AUTOMERGEPULLREQUEST
     */
    enum class OpType(val value: String) {

        CREATEREPO("create_repo"),

        RENAMEREPO("rename_repo"),

        STARREPO("star_repo"),

        WATCHREPO("watch_repo"),

        COMMITREPO("commit_repo"),

        CREATEISSUE("create_issue"),

        CREATEPULLREQUEST("create_pull_request"),

        TRANSFERREPO("transfer_repo"),

        PUSHTAG("push_tag"),

        COMMENTISSUE("comment_issue"),

        MERGEPULLREQUEST("merge_pull_request"),

        CLOSEISSUE("close_issue"),

        REOPENISSUE("reopen_issue"),

        CLOSEPULLREQUEST("close_pull_request"),

        REOPENPULLREQUEST("reopen_pull_request"),

        DELETETAG("delete_tag"),

        DELETEBRANCH("delete_branch"),

        MIRRORSYNCPUSH("mirror_sync_push"),

        MIRRORSYNCCREATE("mirror_sync_create"),

        MIRRORSYNCDELETE("mirror_sync_delete"),

        APPROVEPULLREQUEST("approve_pull_request"),

        REJECTPULLREQUEST("reject_pull_request"),

        COMMENTPULL("comment_pull"),

        PUBLISHRELEASE("publish_release"),

        PULLREVIEWDISMISSED("pull_review_dismissed"),

        PULLREQUESTREADYFORREVIEW("pull_request_ready_for_review"),

        AUTOMERGEPULLREQUEST("auto_merge_pull_request");

    }


}

