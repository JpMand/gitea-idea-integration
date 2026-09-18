package com.github.jpmand.idea.plugin.gitea.api.rest.dto


/**
 * ExternalWiki represents setting for external wiki
 * @param externalWikiUrl URL of external wiki.
 */
data class ExternalWiki(
    /* URL of external wiki. */
    val externalWikiUrl: String? = null,
)

