package com.github.jpmand.idea.plugin.gitea.api.rest.dto

/**
 * Branch represents a repository branch
 * @param commit
 * @param effectiveBranchProtectionName EffectiveBranchProtectionName is the name of the effective branch protection rule
 * @param enableStatusCheck EnableStatusCheck indicates if status checks are enabled
 * @param name Name is the branch name
 * @param &#x60;protected&#x60; Protected indicates if the branch is protected
 * @param requiredApprovals RequiredApprovals is the number of required approvals for pull requests
 * @param statusCheckContexts StatusCheckContexts contains the list of required status check contexts
 * @param userCanMerge UserCanMerge indicates if the current user can merge to this branch
 * @param userCanPush UserCanPush indicates if the current user can push to this branch
 */
data class Branch(
    val commit: PayloadCommit? = null,
    /* EffectiveBranchProtectionName is the name of the effective branch protection rule */
    val effectiveBranchProtectionName: String? = null,
    /* EnableStatusCheck indicates if status checks are enabled */
    val enableStatusCheck: Boolean? = null,
    /* Name is the branch name */
    val name: String? = null,
    /* Protected indicates if the branch is protected */
    val `protected`: Boolean? = null,
    /* RequiredApprovals is the number of required approvals for pull requests */
    val requiredApprovals: Long? = null,
    /* StatusCheckContexts contains the list of required status check contexts */
    val statusCheckContexts: Array<String>? = null,
    /* UserCanMerge indicates if the current user can merge to this branch */
    val userCanMerge: Boolean? = null,
    /* UserCanPush indicates if the current user can push to this branch */
    val userCanPush: Boolean? = null,
)

