package com.github.jpmand.idea.plugin.gitea.api.rest.dto

/**
 * SearchResults results of a successful search
 * @param &#x60;data&#x60; Data contains the repository search results
 * @param ok OK indicates if the search was successful
 */
data class SearchResults(
    /* Data contains the repository search results */
    val `data`: Array<Repository>? = null,
    /* OK indicates if the search was successful */
    val ok: Boolean? = null,
)

