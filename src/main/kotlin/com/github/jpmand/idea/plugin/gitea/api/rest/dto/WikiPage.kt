package com.github.jpmand.idea.plugin.gitea.api.rest.dto

/**
 * WikiPage a wiki page
 * @param commitCount The number of commits that modified this page
 * @param contentBase64 Page content, base64 encoded
 * @param footer The footer content for the wiki page
 * @param htmlUrl The HTML URL to view the wiki page
 * @param lastCommit
 * @param sidebar The sidebar content for the wiki page
 * @param subUrl The sub URL path for the wiki page
 * @param title The title of the wiki page
 */
data class WikiPage(
    /* The number of commits that modified this page */
    val commitCount: Long? = null,
    /* Page content, base64 encoded */
    val contentBase64: String? = null,
    /* The footer content for the wiki page */
    val footer: String? = null,
    /* The HTML URL to view the wiki page */
    val htmlUrl: String? = null,
    val lastCommit: WikiCommit? = null,
    /* The sidebar content for the wiki page */
    val sidebar: String? = null,
    /* The sub URL path for the wiki page */
    val subUrl: String? = null,
    /* The title of the wiki page */
    val title: String? = null,
)

