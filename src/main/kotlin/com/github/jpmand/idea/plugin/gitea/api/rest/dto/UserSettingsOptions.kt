package com.github.jpmand.idea.plugin.gitea.api.rest.dto


/**
 * UserSettingsOptions represents options to change user settings
 * @param description
 * @param diffViewStyle
 * @param fullName
 * @param hideActivity
 * @param hideEmail Privacy
 * @param language
 * @param location
 * @param theme
 * @param website
 */
data class UserSettingsOptions(
    val description: String? = null,
    val diffViewStyle: String? = null,
    val fullName: String? = null,
    val hideActivity: Boolean? = null,
    /* Privacy */
    val hideEmail: Boolean? = null,
    val language: String? = null,
    val location: String? = null,
    val theme: String? = null,
    val website: String? = null,
)

