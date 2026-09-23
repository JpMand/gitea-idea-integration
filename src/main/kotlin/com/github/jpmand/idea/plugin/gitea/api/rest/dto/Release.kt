package com.github.jpmand.idea.plugin.gitea.api.rest.dto

import java.time.OffsetDateTime

/**
 * Release represents a repository release
 * @param assets The files attached to the release
 * @param author
 * @param body The release notes or description
 * @param createdAt
 * @param draft Whether the release is a draft
 * @param htmlUrl The HTML URL to view the release
 * @param id The unique identifier of the release
 * @param name The display title of the release
 * @param prerelease Whether the release is a prerelease
 * @param publishedAt
 * @param tagName The name of the git tag associated with the release
 * @param tarballUrl The URL to download the tarball archive
 * @param targetCommitish The target commitish for the release
 * @param uploadUrl The URL template for uploading release assets
 * @param url The API URL of the release
 * @param zipballUrl The URL to download the zip archive
 */
data class Release(
    /* The files attached to the release */
    val assets: Array<Attachment>? = null,
    val author: User? = null,
    /* The release notes or description */
    val body: String? = null,
    val createdAt: OffsetDateTime? = null,
    /* Whether the release is a draft */
    val draft: Boolean? = null,
    /* The HTML URL to view the release */
    val htmlUrl: String? = null,
    /* The unique identifier of the release */
    val id: Long? = null,
    /* The display title of the release */
    val name: String? = null,
    /* Whether the release is a prerelease */
    val prerelease: Boolean? = null,
    val publishedAt: OffsetDateTime? = null,
    /* The name of the git tag associated with the release */
    val tagName: String? = null,
    /* The URL to download the tarball archive */
    val tarballUrl: String? = null,
    /* The target commitish for the release */
    val targetCommitish: String? = null,
    /* The URL template for uploading release assets */
    val uploadUrl: String? = null,
    /* The API URL of the release */
    val url: String? = null,
    /* The URL to download the zip archive */
    val zipballUrl: String? = null,
)

