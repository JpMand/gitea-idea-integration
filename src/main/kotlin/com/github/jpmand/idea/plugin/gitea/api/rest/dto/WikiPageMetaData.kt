package com.github.jpmand.idea.plugin.gitea.api.rest.dto

/**
 * WikiPageMetaData wiki page meta information
 * @param htmlUrl The HTML URL to view the wiki page
 * @param lastCommit
 * @param subUrl The sub URL path for the wiki page
 * @param title The title of the wiki page
 */
data class WikiPageMetaData(
    /* The HTML URL to view the wiki page */
    val htmlUrl: String? = null,
    val lastCommit: WikiCommit? = null,
    /* The sub URL path for the wiki page */
    val subUrl: String? = null,
    /* The title of the wiki page */
    val title: String? = null,
)

