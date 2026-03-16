package com.github.jpmand.idea.plugin.gitea.api.rest.models

import java.util.Date

open class GiteaRepositoryDTO(
  val id: Int,
  val owner: GiteaUserDTO,
  val name: String,
  val fullName: String,
  val description: String,
  val empty: Boolean,
  val htmlUrl: String,
  val url: String,
  val sshUrl: String,
  val cloneUrl: String,
  val originalUrl: String,
  val defaultBranch: String,
  val defaultTargetBranch: String?,
  val createdAt: Date,
  val updatedAt: Date?,
  val hasCode: Boolean,
  val hasIssues: Boolean,
  val hasPullRequests: Boolean,
  val openIssuesCount: Int,
  val openPrCounter: Int,
  val allowMergeCommits: Boolean,
  val allowRebase: Boolean,
  val allowRebaseExplicit: Boolean,
  val allowSquashMerge: Boolean,
  val allowFastForwardOnlyMerge: Boolean,
  val allowRebaseUpdate: Boolean,
  val allowManualMerge: Boolean,
  val autodetectManualMerge: Boolean,
  val defaultDeleteBranchAfterMerge: Boolean,
  val defaultMergeStyle: String,
  val defaultAllowMaintainerEdit: Boolean
  ) {

  override fun toString(): String {
    return "GiteaRepositoryDTO(id=$id, owner=${owner.login}, name='$name', fullName='$fullName', description='$description', empty=$empty, htmlUrl='$htmlUrl', url='$url', sshUrl='$sshUrl', cloneUrl='$cloneUrl', originalUrl='$originalUrl', defaultBranch='$defaultBranch', defaultTargetBranch='$defaultTargetBranch' createdAt=$createdAt, updatedAt=$updatedAt, hasCode=$hasCode, hasIssues=$hasIssues, hasPullRequests=$hasPullRequests, openIssuesCount=$openIssuesCount, openPrCounter=$openPrCounter, allowMergeCommits=$allowMergeCommits, allowRebase=$allowRebase, allowRebaseExplicit=$allowRebaseExplicit, allowSquashMerge=$allowSquashMerge, allowFastForwardOnlyMerge=$allowFastForwardOnlyMerge, allowRebaseUpdate=$allowRebaseUpdate, allowManualMerge=$allowManualMerge, autodetectManualMerge=$autodetectManualMerge, defaultDeleteBranchAfterMerge=$defaultDeleteBranchAfterMerge, defaultMergeStyle='$defaultMergeStyle', defaultAllowMaintainerEdit=$defaultAllowMaintainerEdit)"
  }
}