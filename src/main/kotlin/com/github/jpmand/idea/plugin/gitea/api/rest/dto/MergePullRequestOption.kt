package com.github.jpmand.idea.plugin.gitea.api.rest.dto

import com.fasterxml.jackson.annotation.JsonProperty


/**
 * MergePullRequestForm form for merging Pull Request
 * @param deleteBranchAfterMerge
 * @param &#x60;do&#x60; 
 * @param forceMerge
 * @param headCommitId
 * @param mergeCommitId
 * @param mergeMessageField
 * @param mergeTitleField
 * @param mergeWhenChecksSucceed
 */
data class MergePullRequestOption(
    val deleteBranchAfterMerge: Boolean? = null,
    val `do`: Do,
    val forceMerge: Boolean? = null,
    val headCommitId: String? = null,
    val mergeCommitId: String? = null,
    val mergeMessageField: String? = null,
    val mergeTitleField: String? = null,
    val mergeWhenChecksSucceed: Boolean? = null,
) {


    /**
     *
     * Values: MERGE,REBASE,REBASEMERGE,SQUASH,FASTFORWARDONLY,MANUALLYMERGED
     */
    enum class Do(val value: kotlin.String)
    {

        @JsonProperty("merge") MERGE("merge"),

        @JsonProperty("rebase") REBASE("rebase"),

        @JsonProperty("rebase-merge") REBASEMERGE("rebase-merge"),

        @JsonProperty("squash") SQUASH("squash"),

        @JsonProperty("fast-forward-only") FASTFORWARDONLY("fast-forward-only"),

        @JsonProperty("manually-merged") MANUALLYMERGED("manually-merged")

    }


}

