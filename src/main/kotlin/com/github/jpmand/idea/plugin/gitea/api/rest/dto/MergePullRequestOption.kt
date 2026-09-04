package com.github.jpmand.idea.plugin.gitea.api.rest.dto


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

        MERGE("merge"),

        REBASE("rebase"),

        REBASEMERGE("rebase-merge"),

        SQUASH("squash"),

        FASTFORWARDONLY("fast-forward-only"),

        MANUALLYMERGED("manually-merged")

    }


}

