package com.github.jpmand.idea.plugin.gitea.api.rest.dto


/**
 * MigrateRepoOptions options for migrating repository's this is used to interact with api v1
 * @param authPassword
 * @param authToken
 * @param authUsername
 * @param awsAccessKeyId
 * @param awsSecretAccessKey
 * @param cloneAddr
 * @param description
 * @param issues
 * @param labels
 * @param lfs
 * @param lfsEndpoint
 * @param milestones
 * @param mirror
 * @param mirrorInterval
 * @param &#x60;private&#x60; 
 * @param pullRequests
 * @param releases
 * @param repoName
 * @param repoOwner the organization's name or individual user's name who will own the migrated repository
 * @param service
 * @param uid deprecated (only for backwards compatibility, use repo_owner instead)
 * @param wiki
 */
data class MigrateRepoOptions(
    val authPassword: String? = null,
    val authToken: String? = null,
    val authUsername: String? = null,
    val awsAccessKeyId: String? = null,
    val awsSecretAccessKey: String? = null,
    val cloneAddr: String,
    val description: String? = null,
    val issues: Boolean? = null,
    val labels: Boolean? = null,
    val lfs: Boolean? = null,
    val lfsEndpoint: String? = null,
    val milestones: Boolean? = null,
    val mirror: Boolean? = null,
    val mirrorInterval: String? = null,
    val `private`: Boolean? = null,
    val pullRequests: Boolean? = null,
    val releases: Boolean? = null,
    val repoName: String,
    /* the organization's name or individual user's name who will own the migrated repository */
    val repoOwner: String? = null,
    val service: Service? = null,
    /* deprecated (only for backwards compatibility, use repo_owner instead) */
    val uid: Long? = null,
    val wiki: Boolean? = null,
) {


    /**
     *
     * Values: GIT,GITHUB,GITEA,GITLAB,GOGS,ONEDEV,GITBUCKET,CODEBASE,CODECOMMIT
     */
    enum class Service(val value: String) {

        GIT("git"),

        GITHUB("github"),

        GITEA("gitea"),

        GITLAB("gitlab"),

        GOGS("gogs"),

        ONEDEV("onedev"),

        GITBUCKET("gitbucket"),

        CODEBASE("codebase"),

        CODECOMMIT("codecommit");

    }


}

