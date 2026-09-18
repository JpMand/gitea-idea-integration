package com.github.jpmand.idea.plugin.gitea.api.rest.dto


/**
 * EditReleaseOption options when editing a release
 * @param body The new release notes or description
 * @param draft Whether to change the draft status
 * @param name The new display title of the release
 * @param prerelease Whether to change the prerelease status
 * @param tagName The new name of the git tag
 * @param targetCommitish The new target commitish for the release
 */
data class EditReleaseOption(
    /* The new release notes or description */
    val body: String? = null,
    /* Whether to change the draft status */
    val draft: Boolean? = null,
    /* The new display title of the release */
    val name: String? = null,
    /* Whether to change the prerelease status */
    val prerelease: Boolean? = null,
    /* The new name of the git tag */
    val tagName: String? = null,
    /* The new target commitish for the release */
    val targetCommitish: String? = null,
)

