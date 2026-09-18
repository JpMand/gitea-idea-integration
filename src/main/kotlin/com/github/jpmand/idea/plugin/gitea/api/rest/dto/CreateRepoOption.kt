package com.github.jpmand.idea.plugin.gitea.api.rest.dto


/**
 * CreateRepoOption options when creating repository
 * @param autoInit Whether the repository should be auto-initialized?
 * @param defaultBranch DefaultBranch of the repository (used when initializes and in template)
 * @param description Description of the repository to create
 * @param gitignores Gitignores to use
 * @param issueLabels Label-Set to use
 * @param license License to use
 * @param name Name of the repository to create
 * @param objectFormatName ObjectFormatName of the underlying git repository, empty string for default (sha1)
 * @param &#x60;private&#x60; Whether the repository is private
 * @param readme Readme of the repository to create
 * @param template Whether the repository is template
 * @param trustModel TrustModel of the repository
 */
data class CreateRepoOption(
    /* Whether the repository should be auto-initialized? */
    val autoInit: Boolean? = null,
    /* DefaultBranch of the repository (used when initializes and in template) */
    val defaultBranch: String? = null,
    /* Description of the repository to create */
    val description: String? = null,
    /* Gitignores to use */
    val gitignores: String? = null,
    /* Label-Set to use */
    val issueLabels: String? = null,
    /* License to use */
    val license: String? = null,
    /* Name of the repository to create */
    val name: String,
    /* ObjectFormatName of the underlying git repository, empty string for default (sha1) */
    val objectFormatName: ObjectFormatName? = null,
    /* Whether the repository is private */
    val `private`: Boolean? = null,
    /* Readme of the repository to create */
    val readme: String? = null,
    /* Whether the repository is template */
    val template: Boolean? = null,
    /* TrustModel of the repository */
    val trustModel: TrustModel? = null,
) {


    /**
     * ObjectFormatName of the underlying git repository, empty string for default (sha1)
     * Values: SHA1,SHA256
     */
    enum class ObjectFormatName(val value: String) {

        SHA1("sha1"),

        SHA256("sha256");

    }


    /**
     * TrustModel of the repository
     * Values: DEFAULT,COLLABORATOR,COMMITTER,COLLABORATORCOMMITTER
     */
    enum class TrustModel(val value: String) {

        DEFAULT("default"),

        COLLABORATOR("collaborator"),

        COMMITTER("committer"),

        COLLABORATORCOMMITTER("collaboratorcommitter");

    }


}

