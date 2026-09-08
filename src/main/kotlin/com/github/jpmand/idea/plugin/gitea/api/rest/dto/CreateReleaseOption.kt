package com.github.jpmand.idea.plugin.gitea.api.rest.dto


/**
 * CreateReleaseOption options when creating a release
 * @param body The release notes or description
 * @param draft Whether to create the release as a draft
 * @param name The display title of the release
 * @param prerelease Whether to mark the release as a prerelease
 * @param tagMessage The message for the git tag
 * @param tagName
 * @param targetCommitish The target commitish for the release
 */
data class CreateReleaseOption(
    /* The release notes or description */
    val body: String? = null,
    /* Whether to create the release as a draft */
    val draft: Boolean? = null,
    /* The display title of the release */
    val name: String? = null,
    /* Whether to mark the release as a prerelease */
    val prerelease: Boolean? = null,
    /* The message for the git tag */
    val tagMessage: String? = null,
    val tagName: String,
    /* The target commitish for the release */
    val targetCommitish: String? = null,
)

