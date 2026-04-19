package com.github.jpmand.idea.plugin.gitea.api.rest.models.pr

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Request body for POST /repos/{owner}/{repo}/pulls/{index}/merge (MergePullRequestOption).
 *
 * The Gitea API uses PascalCase JSON keys for the merge-specific fields (`Do`, `MergeCommitID`,
 * `MergeMessageField`, `MergeTitleField`) — these are direct Go exported-field names.
 * Jackson's SNAKE_CASE strategy is overridden per-field with @field:JsonProperty.
 */
class GiteaMergePullRequestRequestDTO(
    @field:JsonProperty("Do")
    val doStrategy: GiteaMergeStyleEnum,
    @field:JsonProperty("MergeMessageField")
    val mergeMessageField: String? = null,
    @field:JsonProperty("MergeTitleField")
    val mergeTitleField: String? = null,
    @field:JsonProperty("MergeCommitID")
    val mergeCommitId: String? = null,
    val deleteBranchAfterMerge: Boolean? = null,
    val forceMerge: Boolean? = null,
    val headCommitId: String? = null,
    val mergeWhenChecksSucceed: Boolean? = null,
)
