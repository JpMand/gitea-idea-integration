package com.github.jpmand.idea.plugin.gitea.api.rest.dto

import java.time.OffsetDateTime

/**
 * Repository represents a repository
 * @param allowFastForwardOnlyMerge
 * @param allowManualMerge
 * @param allowMergeCommits
 * @param allowMergeUpdate
 * @param allowRebase
 * @param allowRebaseExplicit
 * @param allowRebaseUpdate
 * @param allowSquashMerge
 * @param archived
 * @param archivedAt
 * @param autodetectManualMerge
 * @param avatarUrl
 * @param branchCount
 * @param cloneUrl
 * @param createdAt
 * @param defaultAllowMaintainerEdit
 * @param defaultBranch
 * @param defaultDeleteBranchAfterMerge
 * @param defaultMergeStyle
 * @param defaultTargetBranch
 * @param defaultUpdateStyle
 * @param description
 * @param empty
 * @param externalTracker
 * @param externalWiki
 * @param fork
 * @param forksCount
 * @param fullName
 * @param hasActions
 * @param hasCode
 * @param hasIssues
 * @param hasPackages
 * @param hasProjects
 * @param hasPullRequests
 * @param hasReleases
 * @param hasWiki
 * @param htmlUrl
 * @param id
 * @param ignoreWhitespaceConflicts
 * @param &#x60;internal&#x60; 
 * @param internalTracker
 * @param language
 * @param languagesUrl
 * @param licenses
 * @param link
 * @param mirror
 * @param mirrorInterval
 * @param mirrorLastSyncAt
 * @param mirrorUpdated
 * @param name
 * @param objectFormatName ObjectFormatName of the underlying git repository
 * @param openIssuesCount
 * @param openPrCounter
 * @param originalUrl
 * @param owner
 * @param parent
 * @param permissions
 * @param &#x60;private&#x60; 
 * @param projectsMode
 * @param releaseCounter
 * @param repoTransfer
 * @param size
 * @param sshUrl
 * @param starsCount
 * @param template
 * @param topics
 * @param updatedAt
 * @param url
 * @param watchersCount
 * @param website
 */
data class Repository(
    val allowFastForwardOnlyMerge: Boolean? = null,
    val allowManualMerge: Boolean? = null,
    val allowMergeCommits: Boolean? = null,
    val allowMergeUpdate: Boolean? = null,
    val allowRebase: Boolean? = null,
    val allowRebaseExplicit: Boolean? = null,
    val allowRebaseUpdate: Boolean? = null,
    val allowSquashMerge: Boolean? = null,
    val archived: Boolean? = null,
    val archivedAt: OffsetDateTime? = null,
    val autodetectManualMerge: Boolean? = null,
    val avatarUrl: String? = null,
    val branchCount: Long? = null,
    val cloneUrl: String? = null,
    val createdAt: OffsetDateTime? = null,
    val defaultAllowMaintainerEdit: Boolean? = null,
    val defaultBranch: String? = null,
    val defaultDeleteBranchAfterMerge: Boolean? = null,
    val defaultMergeStyle: String? = null,
    val defaultTargetBranch: String? = null,
    val defaultUpdateStyle: String? = null,
    val description: String? = null,
    val empty: Boolean? = null,
    val externalTracker: ExternalTracker? = null,
    val externalWiki: ExternalWiki? = null,
    val fork: Boolean? = null,
    val forksCount: Long? = null,
    val fullName: String? = null,
    val hasActions: Boolean? = null,
    val hasCode: Boolean? = null,
    val hasIssues: Boolean? = null,
    val hasPackages: Boolean? = null,
    val hasProjects: Boolean? = null,
    val hasPullRequests: Boolean? = null,
    val hasReleases: Boolean? = null,
    val hasWiki: Boolean? = null,
    val htmlUrl: String? = null,
    val id: Long? = null,
    val ignoreWhitespaceConflicts: Boolean? = null,
    val `internal`: Boolean? = null,
    val internalTracker: InternalTracker? = null,
    val language: String? = null,
    val languagesUrl: String? = null,
    val licenses: Array<String>? = null,
    val link: String? = null,
    val mirror: Boolean? = null,
    val mirrorInterval: String? = null,
    val mirrorLastSyncAt: OffsetDateTime? = null,
    val mirrorUpdated: OffsetDateTime? = null,
    val name: String? = null,
    /* ObjectFormatName of the underlying git repository */
    val objectFormatName: ObjectFormatName? = null,
    val openIssuesCount: Long? = null,
    val openPrCounter: Long? = null,
    val originalUrl: String? = null,
    val owner: User? = null,
    val parent: Repository? = null,
    val permissions: Permission? = null,
    val `private`: Boolean? = null,
    val projectsMode: String? = null,
    val releaseCounter: Long? = null,
    val repoTransfer: RepoTransfer? = null,
    val size: Long? = null,
    val sshUrl: String? = null,
    val starsCount: Long? = null,
    val template: Boolean? = null,
    val topics: Array<String>? = null,
    val updatedAt: OffsetDateTime? = null,
    val url: String? = null,
    val watchersCount: Long? = null,
    val website: String? = null,
) {


    /**
     * ObjectFormatName of the underlying git repository
     * Values: SHA1,SHA256
     */
    enum class ObjectFormatName(val value: String) {

        SHA1("sha1"),

        SHA256("sha256");

    }


}

