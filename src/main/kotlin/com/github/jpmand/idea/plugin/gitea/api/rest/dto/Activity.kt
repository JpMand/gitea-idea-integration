package com.github.jpmand.idea.plugin.gitea.api.rest.dto

import com.fasterxml.jackson.annotation.JsonProperty
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

        @JsonProperty("create_repo") CREATEREPO("create_repo"),

        @JsonProperty("rename_repo") RENAMEREPO("rename_repo"),

        @JsonProperty("star_repo") STARREPO("star_repo"),

        @JsonProperty("watch_repo") WATCHREPO("watch_repo"),

        @JsonProperty("commit_repo") COMMITREPO("commit_repo"),

        @JsonProperty("create_issue") CREATEISSUE("create_issue"),

        @JsonProperty("create_pull_request") CREATEPULLREQUEST("create_pull_request"),

        @JsonProperty("transfer_repo") TRANSFERREPO("transfer_repo"),

        @JsonProperty("push_tag") PUSHTAG("push_tag"),

        @JsonProperty("comment_issue") COMMENTISSUE("comment_issue"),

        @JsonProperty("merge_pull_request") MERGEPULLREQUEST("merge_pull_request"),

        @JsonProperty("close_issue") CLOSEISSUE("close_issue"),

        @JsonProperty("reopen_issue") REOPENISSUE("reopen_issue"),

        @JsonProperty("close_pull_request") CLOSEPULLREQUEST("close_pull_request"),

        @JsonProperty("reopen_pull_request") REOPENPULLREQUEST("reopen_pull_request"),

        @JsonProperty("delete_tag") DELETETAG("delete_tag"),

        @JsonProperty("delete_branch") DELETEBRANCH("delete_branch"),

        @JsonProperty("mirror_sync_push") MIRRORSYNCPUSH("mirror_sync_push"),

        @JsonProperty("mirror_sync_create") MIRRORSYNCCREATE("mirror_sync_create"),

        @JsonProperty("mirror_sync_delete") MIRRORSYNCDELETE("mirror_sync_delete"),

        @JsonProperty("approve_pull_request") APPROVEPULLREQUEST("approve_pull_request"),

        @JsonProperty("reject_pull_request") REJECTPULLREQUEST("reject_pull_request"),

        @JsonProperty("comment_pull") COMMENTPULL("comment_pull"),

        @JsonProperty("publish_release") PUBLISHRELEASE("publish_release"),

        @JsonProperty("pull_review_dismissed") PULLREVIEWDISMISSED("pull_review_dismissed"),

        @JsonProperty("pull_request_ready_for_review") PULLREQUESTREADYFORREVIEW("pull_request_ready_for_review"),

        @JsonProperty("auto_merge_pull_request") AUTOMERGEPULLREQUEST("auto_merge_pull_request");

    }


}

