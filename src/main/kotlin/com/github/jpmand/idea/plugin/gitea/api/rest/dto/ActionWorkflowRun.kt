package com.github.jpmand.idea.plugin.gitea.api.rest.dto

import java.time.OffsetDateTime

/**
 * ActionWorkflowRun represents a WorkflowRun
 * @param actor
 * @param completedAt
 * @param conclusion
 * @param displayTitle
 * @param event
 * @param headBranch
 * @param headRepository
 * @param headSha
 * @param htmlUrl
 * @param id
 * @param path
 * @param previousAttemptUrl PreviousAttemptURL is the API URL of the previous attempt of this run, e.g. \".../actions/runs/{run_id}/attempts/{attempt-1}\". It is set only when the current attempt is > 1 (i.e. a rerun). For the first attempt, or for legacy runs that pre-date ActionRunAttempt, it is null.
 * @param pullRequests
 * @param repository
 * @param repositoryId
 * @param runAttempt RunAttempt is 1-based for runs created after ActionRunAttempt was introduced. A value of 0 is a legacy-only sentinel for runs created before attempts existed and indicates no corresponding /attempts/{n} resource is available.
 * @param runNumber
 * @param startedAt
 * @param status
 * @param triggerActor
 * @param url
 */
data class ActionWorkflowRun(
    val actor: User? = null,
    val completedAt: OffsetDateTime? = null,
    val conclusion: String? = null,
    val displayTitle: String? = null,
    val event: String? = null,
    val headBranch: String? = null,
    val headRepository: Repository? = null,
    val headSha: String? = null,
    val htmlUrl: String? = null,
    val id: Long? = null,
    val path: String? = null,
    /* PreviousAttemptURL is the API URL of the previous attempt of this run, e.g. \".../actions/runs/{run_id}/attempts/{attempt-1}\". It is set only when the current attempt is > 1 (i.e. a rerun). For the first attempt, or for legacy runs that pre-date ActionRunAttempt, it is null. */
    val previousAttemptUrl: String? = null,
    val pullRequests: Array<PullRequestMinimal>? = null,
    val repository: Repository? = null,
    val repositoryId: Long? = null,
    /* RunAttempt is 1-based for runs created after ActionRunAttempt was introduced. A value of 0 is a legacy-only sentinel for runs created before attempts existed and indicates no corresponding /attempts/{n} resource is available. */
    val runAttempt: Long? = null,
    val runNumber: Long? = null,
    val startedAt: OffsetDateTime? = null,
    val status: String? = null,
    val triggerActor: User? = null,
    val url: String? = null,
)

