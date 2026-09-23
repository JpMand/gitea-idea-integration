package com.github.jpmand.idea.plugin.gitea.api.rest.dto

/**
 * EditRepoOption options when editing a repository's properties
 * @param allowFastForwardOnlyMerge either `true` to allow fast-forward-only merging pull requests, or `false` to prevent fast-forward-only merging.
 * @param allowManualMerge either `true` to allow mark pr as merged manually, or `false` to prevent it.
 * @param allowMergeCommits either `true` to allow merging pull requests with a merge commit, or `false` to prevent merging pull requests with merge commits.
 * @param allowMergeUpdate either `true` to allow updating pull request branch by merge, or `false` to prevent it.
 * @param allowRebase either `true` to allow rebase-merging pull requests, or `false` to prevent rebase-merging.
 * @param allowRebaseExplicit either `true` to allow rebase with explicit merge commits (--no-ff), or `false` to prevent rebase with explicit merge commits.
 * @param allowRebaseUpdate either `true` to allow updating pull request branch by rebase, or `false` to prevent it.
 * @param allowSquashMerge either `true` to allow squash-merging pull requests, or `false` to prevent squash-merging.
 * @param archived set to `true` to archive this repository.
 * @param autodetectManualMerge either `true` to enable AutodetectManualMerge, or `false` to prevent it. Note: In some special cases, misjudgments can occur.
 * @param defaultAllowMaintainerEdit set to `true` to allow edits from maintainers by default
 * @param defaultBranch sets the default branch for this repository.
 * @param defaultDeleteBranchAfterMerge set to `true` to delete pr branch after merge by default
 * @param defaultMergeStyle set to a merge style to be used by this repository: \"merge\", \"rebase\", \"rebase-merge\", \"squash\", or \"fast-forward-only\".
 * @param defaultUpdateStyle set to an update style to be used by this repository: \"merge\" or \"rebase\".
 * @param description a short description of the repository.
 * @param enablePrune enable prune - remove obsolete remote-tracking references when mirroring
 * @param externalTracker
 * @param externalWiki
 * @param hasActions either `true` to enable actions unit, or `false` to disable them.
 * @param hasCode either `true` to enable code for this repository or `false` to disable it.
 * @param hasIssues either `true` to enable issues for this repository or `false` to disable them.
 * @param hasPackages either `true` to enable packages unit, or `false` to disable them.
 * @param hasProjects either `true` to enable project unit, or `false` to disable them.
 * @param hasPullRequests either `true` to allow pull requests, or `false` to prevent pull request.
 * @param hasReleases either `true` to enable releases unit, or `false` to disable them.
 * @param hasWiki either `true` to enable the wiki for this repository or `false` to disable it.
 * @param ignoreWhitespaceConflicts either `true` to ignore whitespace for conflicts, or `false` to not ignore whitespace.
 * @param internalTracker
 * @param mirrorInterval set to a string like `8h30m0s` to set the mirror interval time
 * @param mirrorPassword authentication password for the remote repository (mirrors)
 * @param mirrorToken authentication token for the remote repository (mirrors)
 * @param mirrorUsername authentication username for the remote repository (mirrors)
 * @param name name of the repository
 * @param &#x60;private&#x60; either `true` to make the repository private or `false` to make it public. Note: you will get a 422 error if the organization restricts changing repository visibility to organization owners and a non-owner tries to change the value of private.
 * @param projectsMode `repo` to only allow repo-level projects, `owner` to only allow owner projects, `all` to allow both.
 * @param template either `true` to make this repository a template or `false` to make it a normal repository
 * @param website a URL with more information about the repository.
 */
data class EditRepoOption(
    /* either `true` to allow fast-forward-only merging pull requests, or `false` to prevent fast-forward-only merging. */
    val allowFastForwardOnlyMerge: Boolean? = null,
    /* either `true` to allow mark pr as merged manually, or `false` to prevent it. */
    val allowManualMerge: Boolean? = null,
    /* either `true` to allow merging pull requests with a merge commit, or `false` to prevent merging pull requests with merge commits. */
    val allowMergeCommits: Boolean? = null,
    /* either `true` to allow updating pull request branch by merge, or `false` to prevent it. */
    val allowMergeUpdate: Boolean? = null,
    /* either `true` to allow rebase-merging pull requests, or `false` to prevent rebase-merging. */
    val allowRebase: Boolean? = null,
    /* either `true` to allow rebase with explicit merge commits (--no-ff), or `false` to prevent rebase with explicit merge commits. */
    val allowRebaseExplicit: Boolean? = null,
    /* either `true` to allow updating pull request branch by rebase, or `false` to prevent it. */
    val allowRebaseUpdate: Boolean? = null,
    /* either `true` to allow squash-merging pull requests, or `false` to prevent squash-merging. */
    val allowSquashMerge: Boolean? = null,
    /* set to `true` to archive this repository. */
    val archived: Boolean? = null,
    /* either `true` to enable AutodetectManualMerge, or `false` to prevent it. Note: In some special cases, misjudgments can occur. */
    val autodetectManualMerge: Boolean? = null,
    /* set to `true` to allow edits from maintainers by default */
    val defaultAllowMaintainerEdit: Boolean? = null,
    /* sets the default branch for this repository. */
    val defaultBranch: String? = null,
    /* set to `true` to delete pr branch after merge by default */
    val defaultDeleteBranchAfterMerge: Boolean? = null,
    /* set to a merge style to be used by this repository: \"merge\", \"rebase\", \"rebase-merge\", \"squash\", or \"fast-forward-only\". */
    val defaultMergeStyle: String? = null,
    /* set to an update style to be used by this repository: \"merge\" or \"rebase\". */
    val defaultUpdateStyle: String? = null,
    /* a short description of the repository. */
    val description: String? = null,
    /* enable prune - remove obsolete remote-tracking references when mirroring */
    val enablePrune: Boolean? = null,
    val externalTracker: ExternalTracker? = null,
    val externalWiki: ExternalWiki? = null,
    /* either `true` to enable actions unit, or `false` to disable them. */
    val hasActions: Boolean? = null,
    /* either `true` to enable code for this repository or `false` to disable it. */
    val hasCode: Boolean? = null,
    /* either `true` to enable issues for this repository or `false` to disable them. */
    val hasIssues: Boolean? = null,
    /* either `true` to enable packages unit, or `false` to disable them. */
    val hasPackages: Boolean? = null,
    /* either `true` to enable project unit, or `false` to disable them. */
    val hasProjects: Boolean? = null,
    /* either `true` to allow pull requests, or `false` to prevent pull request. */
    val hasPullRequests: Boolean? = null,
    /* either `true` to enable releases unit, or `false` to disable them. */
    val hasReleases: Boolean? = null,
    /* either `true` to enable the wiki for this repository or `false` to disable it. */
    val hasWiki: Boolean? = null,
    /* either `true` to ignore whitespace for conflicts, or `false` to not ignore whitespace. */
    val ignoreWhitespaceConflicts: Boolean? = null,
    val internalTracker: InternalTracker? = null,
    /* set to a string like `8h30m0s` to set the mirror interval time */
    val mirrorInterval: String? = null,
    /* authentication password for the remote repository (mirrors) */
    val mirrorPassword: String? = null,
    /* authentication token for the remote repository (mirrors) */
    val mirrorToken: String? = null,
    /* authentication username for the remote repository (mirrors) */
    val mirrorUsername: String? = null,
    /* name of the repository */
    val name: String? = null,
    /* either `true` to make the repository private or `false` to make it public. Note: you will get a 422 error if the organization restricts changing repository visibility to organization owners and a non-owner tries to change the value of private. */
    val `private`: Boolean? = null,
    /* `repo` to only allow repo-level projects, `owner` to only allow owner projects, `all` to allow both. */
    val projectsMode: String? = null,
    /* either `true` to make this repository a template or `false` to make it a normal repository */
    val template: Boolean? = null,
    /* a URL with more information about the repository. */
    val website: String? = null,
)

