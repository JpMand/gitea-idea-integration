package com.github.jpmand.idea.plugin.gitea.api.rest.dto


/**
 * IssueLabelsOption a collection of labels
 * @param labels Labels can be a list of integers representing label IDs or a list of strings representing label names
 */
data class IssueLabelsOption(
    /* Labels can be a list of integers representing label IDs or a list of strings representing label names */
    val labels: Array<Any>? = null,
)

