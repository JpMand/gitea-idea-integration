package com.github.jpmand.idea.plugin.gitea.api.rest.dto


/**
 * UpdateBranchProtectionPriories a list to update the branch protection rule priorities
 * @param ids
 */
data class UpdateBranchProtectionPriories(
    val ids: Array<Long>? = null,
)

