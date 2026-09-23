package com.github.jpmand.idea.plugin.gitea.api.rest.dto

/**
 * 
 * @param &#x60;object&#x60; 
 * @param ref The name of the Git reference (e.g., refs/heads/main)
 * @param url The URL to access this Git reference
 */
data class Reference(
    val `object`: GitObject? = null,
    /* The name of the Git reference (e.g., refs/heads/main) */
    val ref: String? = null,
    /* The URL to access this Git reference */
    val url: String? = null,
)

