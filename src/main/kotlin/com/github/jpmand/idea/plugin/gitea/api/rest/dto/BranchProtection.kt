package com.github.jpmand.idea.plugin.gitea.api.rest.dto

import java.time.OffsetDateTime


/**
 * BranchProtection represents a branch protection for a repository
 * @param approvalsWhitelistTeams
 * @param approvalsWhitelistUsername
 * @param blockAdminMergeOverride
 * @param blockOnOfficialReviewRequests
 * @param blockOnOutdatedBranch
 * @param blockOnRejectedReviews
 * @param branchName
 * @param bypassAllowlistTeams
 * @param bypassAllowlistUsernames
 * @param createdAt
 * @param dismissStaleApprovals
 * @param enableApprovalsWhitelist
 * @param enableBypassAllowlist
 * @param enableForcePush
 * @param enableForcePushAllowlist
 * @param enableMergeWhitelist
 * @param enablePush
 * @param enablePushWhitelist
 * @param enableStatusCheck
 * @param forcePushAllowlistDeployKeys
 * @param forcePushAllowlistTeams
 * @param forcePushAllowlistUsernames
 * @param ignoreStaleApprovals
 * @param mergeWhitelistTeams
 * @param mergeWhitelistUsernames
 * @param priority Priority is the priority of this branch protection rule
 * @param protectedFilePatterns
 * @param pushWhitelistDeployKeys
 * @param pushWhitelistTeams
 * @param pushWhitelistUsernames
 * @param requireSignedCommits
 * @param requiredApprovals
 * @param ruleName RuleName is the name of the branch protection rule
 * @param statusCheckContexts
 * @param unprotectedFilePatterns
 * @param updatedAt
 */
data class BranchProtection(
    val approvalsWhitelistTeams: Array<String>? = null,
    val approvalsWhitelistUsername: Array<String>? = null,
    val blockAdminMergeOverride: Boolean? = null,
    val blockOnOfficialReviewRequests: Boolean? = null,
    val blockOnOutdatedBranch: Boolean? = null,
    val blockOnRejectedReviews: Boolean? = null,
    val branchName: String? = null,
    val bypassAllowlistTeams: Array<String>? = null,
    val bypassAllowlistUsernames: Array<String>? = null,
    val createdAt: OffsetDateTime? = null,
    val dismissStaleApprovals: Boolean? = null,
    val enableApprovalsWhitelist: Boolean? = null,
    val enableBypassAllowlist: Boolean? = null,
    val enableForcePush: Boolean? = null,
    val enableForcePushAllowlist: Boolean? = null,
    val enableMergeWhitelist: Boolean? = null,
    val enablePush: Boolean? = null,
    val enablePushWhitelist: Boolean? = null,
    val enableStatusCheck: Boolean? = null,
    val forcePushAllowlistDeployKeys: Boolean? = null,
    val forcePushAllowlistTeams: Array<String>? = null,
    val forcePushAllowlistUsernames: Array<String>? = null,
    val ignoreStaleApprovals: Boolean? = null,
    val mergeWhitelistTeams: Array<String>? = null,
    val mergeWhitelistUsernames: Array<String>? = null,
    /* Priority is the priority of this branch protection rule */
    val priority: Long? = null,
    val protectedFilePatterns: String? = null,
    val pushWhitelistDeployKeys: Boolean? = null,
    val pushWhitelistTeams: Array<String>? = null,
    val pushWhitelistUsernames: Array<String>? = null,
    val requireSignedCommits: Boolean? = null,
    val requiredApprovals: Long? = null,
    /* RuleName is the name of the branch protection rule */
    val ruleName: String? = null,
    val statusCheckContexts: Array<String>? = null,
    val unprotectedFilePatterns: String? = null,
    val updatedAt: OffsetDateTime? = null,
)

