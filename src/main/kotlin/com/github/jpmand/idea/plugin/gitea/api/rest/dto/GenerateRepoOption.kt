package com.github.jpmand.idea.plugin.gitea.api.rest.dto


/**
 * GenerateRepoOption options when creating a repository using a template
 * @param avatar include avatar of the template repo
 * @param defaultBranch Default branch of the new repository
 * @param description Description of the repository to create
 * @param gitContent include git content of default branch in template repo
 * @param gitHooks include git hooks in template repo
 * @param labels include labels in template repo
 * @param name
 * @param owner the organization's name or individual user's name who will own the new repository
 * @param &#x60;private&#x60; Whether the repository is private
 * @param protectedBranch include protected branches in template repo
 * @param topics include topics in template repo
 * @param webhooks include webhooks in template repo
 */
data class GenerateRepoOption(
    /* include avatar of the template repo */
    val avatar: Boolean? = null,
    /* Default branch of the new repository */
    val defaultBranch: String? = null,
    /* Description of the repository to create */
    val description: String? = null,
    /* include git content of default branch in template repo */
    val gitContent: Boolean? = null,
    /* include git hooks in template repo */
    val gitHooks: Boolean? = null,
    /* include labels in template repo */
    val labels: Boolean? = null,
    val name: String,
    /* the organization's name or individual user's name who will own the new repository */
    val owner: String,
    /* Whether the repository is private */
    val `private`: Boolean? = null,
    /* include protected branches in template repo */
    val protectedBranch: Boolean? = null,
    /* include topics in template repo */
    val topics: Boolean? = null,
    /* include webhooks in template repo */
    val webhooks: Boolean? = null,
)

