package com.github.jpmand.idea.plugin.gitea.api.rest.dto


/**
 * UserBadgeOption options for link between users and badges
 * @param badgeSlugs
 */
data class UserBadgeOption(
    val badgeSlugs: Array<String>? = null,
)

