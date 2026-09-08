package com.github.jpmand.idea.plugin.gitea.api.rest.dto


/**
 * 
 * @param date Date is the commit date in string format
 * @param email Email is the person's email address
 * @param name Name is the person's name
 */
data class CommitUser(
    /* Date is the commit date in string format */
    val date: String? = null,
    /* Email is the person's email address */
    val email: String? = null,
    /* Name is the person's name */
    val name: String? = null,
)

