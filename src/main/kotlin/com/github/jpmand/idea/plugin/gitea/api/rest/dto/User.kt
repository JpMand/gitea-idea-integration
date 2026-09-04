package com.github.jpmand.idea.plugin.gitea.api.rest.dto

import java.time.OffsetDateTime


/**
 * User represents a user
 * @param active Is user active
 * @param avatarUrl URL to the user's avatar
 * @param created
 * @param description the user's description
 * @param email
 * @param followersCount user counts
 * @param followingCount
 * @param fullName the user's full name
 * @param htmlUrl URL to the user's gitea page
 * @param id the user's id
 * @param isAdmin Is the user an administrator
 * @param language User locale
 * @param lastLogin
 * @param location the user's location
 * @param login login of the user, same as `username`
 * @param loginName identifier of the user, provided by the external authenticator (if configured)
 * @param prohibitLogin Is user login prohibited
 * @param restricted Is user restricted
 * @param sourceId The ID of the user's Authentication Source
 * @param starredReposCount
 * @param visibility User visibility level option: public, limited, private
 * @param website the user's website
 */
data class User(
    /* Is user active */
    val active: Boolean? = null,
    /* URL to the user's avatar */
    val avatarUrl: String? = null,
    val created: OffsetDateTime? = null,
    /* the user's description */
    val description: String? = null,
    val email: String? = null,
    /* user counts */
    val followersCount: Long? = null,
    val followingCount: Long? = null,
    /* the user's full name */
    val fullName: String? = null,
    /* URL to the user's gitea page */
    val htmlUrl: String? = null,
    /* the user's id */
    val id: Long? = null,
    /* Is the user an administrator */
    val isAdmin: Boolean? = null,
    /* User locale */
    val language: String? = null,
    val lastLogin: OffsetDateTime? = null,
    /* the user's location */
    val location: String? = null,
    /* login of the user, same as `username` */
    val login: String? = null,
    /* identifier of the user, provided by the external authenticator (if configured) */
    val loginName: String? = null,
    /* Is user login prohibited */
    val prohibitLogin: Boolean? = null,
    /* Is user restricted */
    val restricted: Boolean? = null,
    /* The ID of the user's Authentication Source */
    val sourceId: Long? = null,
    val starredReposCount: Long? = null,
    /* User visibility level option: public, limited, private */
    val visibility: Visibility? = null,
    /* the user's website */
    val website: String? = null,
) {


    /**
     * User visibility level option: public, limited, private
     * Values: PUBLIC,LIMITED,PRIVATE
     */
    enum class Visibility(val value: String) {

        PUBLIC("public"),

        LIMITED("limited"),

        PRIVATE("private");

    }


}

