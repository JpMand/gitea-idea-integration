package com.github.jpmand.idea.plugin.gitea.api.rest.dto


/**
 * GeneralAPISettings contains global api settings exposed by it
 * @param defaultGitTreesPerPage DefaultGitTreesPerPage is the default number of Git tree items per page
 * @param defaultMaxBlobSize DefaultMaxBlobSize is the default maximum blob size for API responses
 * @param defaultMaxResponseSize DefaultMaxResponseSize is the default maximum response size
 * @param defaultPagingNum DefaultPagingNum is the default number of items per page
 * @param maxResponseItems MaxResponseItems is the maximum number of items returned in API responses
 */
data class GeneralAPISettings(
    /* DefaultGitTreesPerPage is the default number of Git tree items per page */
    val defaultGitTreesPerPage: Long? = null,
    /* DefaultMaxBlobSize is the default maximum blob size for API responses */
    val defaultMaxBlobSize: Long? = null,
    /* DefaultMaxResponseSize is the default maximum response size */
    val defaultMaxResponseSize: Long? = null,
    /* DefaultPagingNum is the default number of items per page */
    val defaultPagingNum: Long? = null,
    /* MaxResponseItems is the maximum number of items returned in API responses */
    val maxResponseItems: Long? = null,
)

